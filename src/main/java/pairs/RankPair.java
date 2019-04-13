package pairs;

import main.ConfigIndexParams;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.meta.CurrencyPairMetaData;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.service.marketdata.MarketDataService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class RankPair {
    CurrencyPair currencyPair;
    CurrencyPairMetaData currencyPairMetaData;
    OrderBook orderBook;
    Ticker ticker;
    double rank;
    // Будем хранить значения рассчитанных индексов, чтобы понимать из чего сложился ранг для пары
    double volumeIndex;
    double askBidDifferenceIndex;
    double PositionIndex;


    public RankPair(CurrencyPair currencyPair, CurrencyPairMetaData currencyPairMetaData, OrderBook orderBook, Ticker ticker){
        this.currencyPair = currencyPair;
        this.currencyPairMetaData = currencyPairMetaData;
        this.orderBook = orderBook;
        this.ticker = ticker;
        this.rank = 1;
    }


    public static List<RankPair> GenerateRankPairList(Map<CurrencyPair, CurrencyPairMetaData> currencyPairs, MarketDataService marketDataService) {
        // Потоками получил по каждой паре список ордеров и тикет
        LinkedList<ThreadGetTicker> threadGetTickerLinkedList = new LinkedList<>();
        for(Map.Entry<CurrencyPair, CurrencyPairMetaData> entry: currencyPairs.entrySet()){
            //TODO Есть случаи, когда возвращается пустые билеты и ордера, т.к. у нас нет пары на бирже такой, понять как их исключить и не слать лишние запросы
            ThreadGetTicker threadGetTicker = new ThreadGetTicker(entry, marketDataService);
            threadGetTickerLinkedList.add(threadGetTicker);
        }
        WaitThread(threadGetTickerLinkedList);

        // Создал объекты рангов и вернул список
        List<RankPair> rankPairList = new ArrayList();
        for(ThreadGetTicker threadGetTicker : threadGetTickerLinkedList){
            CurrencyPair currencyPair = threadGetTicker.currencyPair;
            CurrencyPairMetaData currencyPairMetaData = currencyPairs.get(currencyPair);
            if((null !=  currencyPairMetaData) && (null != threadGetTicker.orderBook) &&(null != threadGetTicker.ticker)){
                RankPair rankPair = new RankPair(currencyPair,currencyPairMetaData, threadGetTicker.orderBook,threadGetTicker.ticker);
                rankPairList.add(rankPair);
            }
        }
        return rankPairList;
    }


    public void CalculateRank(){
        if(ConfigIndexParams.getVolumeIndexАctivity()) this.CalculateVolumeIndex();
        if(ConfigIndexParams.getAskBidDifferenceIndexActivity()) this.CalculateAskBidDifferenceIndex();
        if(ConfigIndexParams.getPositionIndexАctivity()) this.CalculatePositionIndex();
        // индекс доминирования биткоина - хочу тянуть откуда-то (хорошая штука), писать в БД и считать его движение вверх или вниз
    }

    public void CalculatePositionIndex() {
        if(rank<=0) return;
        BigDecimal high = ticker.getHigh();
        BigDecimal low = ticker.getLow();
        BigDecimal last = ticker.getLast();
        if((low.compareTo(BigDecimal.ZERO) > 0) && (low != null)){
            double PositionIndex = high.subtract(last).divide(high.subtract(low),BigDecimal.ROUND_HALF_EVEN).doubleValue();
            this.rank = this.rank  * PositionIndex;
            this.PositionIndex = PositionIndex;
        } else{
            double PositionIndex = 0;
            this.rank = this.rank * PositionIndex;
            this.PositionIndex = PositionIndex;
        }
    }

    public void CalculateAskBidDifferenceIndex() {
        if(rank<=0) return;
        BigDecimal ask = ticker.getAsk();
        BigDecimal bid = ticker.getBid();
        Integer priceScale = currencyPairMetaData.getPriceScale();
        BigDecimal step = BigDecimal.ONE.movePointLeft(priceScale);
        ask = ask.subtract(step);
        bid = bid.add(step);
        BigDecimal askBidDifferenceIndexBD = ask.divide(bid,  RoundingMode.HALF_UP).multiply(new BigDecimal ("100")).subtract(new BigDecimal ("100"));
        this.askBidDifferenceIndex = askBidDifferenceIndexBD.doubleValue();
        this.rank = rank * askBidDifferenceIndex;
    }

    public void CalculateVolumeIndex(){
        if(rank<=0) return;
        BigDecimal limitPercentAsk = ConfigIndexParams.getVolumeIndexLimitPercent().movePointLeft(2).add(BigDecimal.ONE);
        BigDecimal limitPercentBid = BigDecimal.ONE.subtract(ConfigIndexParams.getVolumeIndexLimitPercent().movePointLeft(2));
        List<LimitOrder> asks = this.orderBook.getAsks();
        BigDecimal ask = this.ticker.getAsk();
        BigDecimal limitAskPrice = ask.multiply(limitPercentAsk);
        List<LimitOrder> bids = this.orderBook.getBids();
        BigDecimal bid = this.ticker.getBid();
        BigDecimal limitBidPrice = bid.multiply(limitPercentBid);
        BigDecimal sumOriginalAmountAsk = BigDecimal.ZERO;
        BigDecimal sumOriginalAmountBid = BigDecimal.ZERO;

        for(LimitOrder limitOrder:asks){
            BigDecimal limitPrice = limitOrder.getLimitPrice();
            if(limitPrice.compareTo(limitAskPrice)==1) break;
            BigDecimal originalAmountAsk = limitOrder.getOriginalAmount();
            sumOriginalAmountAsk = sumOriginalAmountAsk.add(originalAmountAsk);
        }

        for(LimitOrder limitOrder:bids){
            BigDecimal limitPrice = limitOrder.getLimitPrice();
            if(limitPrice.compareTo(limitBidPrice)==-1) break;
            BigDecimal originalAmountBid = limitOrder.getOriginalAmount();
            sumOriginalAmountBid = sumOriginalAmountBid.add(originalAmountBid);
        }

        if((sumOriginalAmountAsk.compareTo(BigDecimal.ZERO) > 0) && (sumOriginalAmountAsk != null)){
            double volumeIndex = sumOriginalAmountBid.divide(sumOriginalAmountAsk,1, RoundingMode.HALF_UP).doubleValue();
            this.rank = this.rank  * volumeIndex;
            this.volumeIndex = volumeIndex;
        } else{
            double volumeIndex = 0;
            this.rank = this.rank * volumeIndex;
            this.volumeIndex = volumeIndex;
        }
    }


    // TODO использую метод в двух местах, может его нужно вынести куда-то и сделать обращение единым, а не дублировать в классах
    public static <T> void WaitThread(LinkedList<T> list)   {
        for(T thread:  list) {
            if(((Thread) thread).isAlive())
            {
                try {
                    ((Thread) thread).join();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }


    // TODO Реализацию спер и сильно не вникал - надо прочитать статью про сортировку в джава, как еще можно реализовать и т.д.
    public static class Comparators {
        public static final Comparator<RankPair> RANK = (RankPair o1, RankPair o2) ->
                Double.compare(o2.rank, o1.rank);
    }


    public CurrencyPairMetaData getCurrencyPairMetaData() {
        return currencyPairMetaData;
    }

    public CurrencyPair getCurrencyPair() {
        return currencyPair;
    }

    public Ticker getTicker() {
        return ticker;
    }

    public double getRank() {
        return rank;
    }

}
