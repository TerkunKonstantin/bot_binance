package sale_block;

import main.Config;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.binance.service.BinanceTradeHistoryParams;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.account.Balance;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.meta.CurrencyPairMetaData;
import org.knowm.xchange.dto.meta.ExchangeMetaData;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.dto.trade.UserTrade;
import org.knowm.xchange.service.account.AccountService;
import org.knowm.xchange.service.trade.TradeService;
import pairs.RankPair;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class BalanceScore {
    Map<Currency, Balance> updateBalance;
    ExchangeMetaData exchangeMetaData;
    private TradeService tradeService;
    private AccountService accountService;
    private LinkedList<ThreadOrderPlaceAsk> threadOrderPlaceAsks;
    private LinkedList<ThreadOrderPlaceBid> threadOrderPlaceBids;
    private LinkedList<ThreadOrderCancelBid> ThreadOrderCancelBids;
    private BigDecimal availableBTC;

    public BalanceScore(Exchange binance) throws IOException {
        // получаю необходимые сервисы
        ExchangeMetaData exchangeMetaData = binance.getExchangeMetaData();
        TradeService tradeService = binance.getTradeService();
        AccountService accountService = binance.getAccountService();
        this.exchangeMetaData = exchangeMetaData;
        this.tradeService = tradeService;
        this.accountService = accountService;
        //меняем неизменяемую мапу на обычную
        Map<Currency, Balance> balanceMap = accountService.getAccountInfo().getWallet().getBalances();
        Map<Currency, Balance> currencyBalanceForWork = new HashMap<>(balanceMap);
        this.updateBalance = currencyBalanceForWork;
        availableBTC = updateBalance.get(Currency.BTC).getAvailable();

        this.threadOrderPlaceAsks = new LinkedList<>();
        this.threadOrderPlaceBids = new LinkedList<>();
        this.ThreadOrderCancelBids = new LinkedList<>();

        // Убрал пары долгого хранения из списка продаж
        BalanceRestrictions.RemoveLongStorage(this);

        // Оставил только те пары, которые торгуются с BTC
        BalanceRestrictions.OnlyBTC(this);

        // Посмотреть по каким парам достаточно для продажи, остальные убрать (Проверку делаю по полю minAmount)
        // TODO обработка нулпоинтерэкзепшен, если будет перед OnlyBTC до этого момента (!Обязательно после OnlyBTC!)
        BalanceRestrictions.EnoughForSale(this);
    }

    public LinkedList<ThreadOrderPlaceAsk> getThreadOrderPlaceAsks() {
        return threadOrderPlaceAsks;
    }

    public void orderPlaceAsk(){
        // бегу по парам баланса и запускаю поток на расстановку ордера, передаю ему историю торгов
        for (Map.Entry<Currency, Balance> entry : this.updateBalance.entrySet()) {
            try {
                CurrencyPair currencyPair = new CurrencyPair(entry.getKey(), Config.getCurrency_for_sale());
                List<UserTrade> userTrades = tradeService.getTradeHistory(new BinanceTradeHistoryParams(currencyPair)).getUserTrades();
                for(int i=userTrades.size()-1;i>=0;i--) {
                    UserTrade userTrade = userTrades.get(i);
                    if(Order.OrderType.BID.equals(userTrade.getType())){
                        ThreadOrderPlaceAsk threadOrderPlaceAsk = new ThreadOrderPlaceAsk(entry, exchangeMetaData, userTrade, tradeService);
                        threadOrderPlaceAsks.add(threadOrderPlaceAsk);
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        WaitThread(threadOrderPlaceAsks);
    }

    private int getAvailableBidOrderCount(){
        int bidOrderCount = 0;
        BigDecimal minRate = Config.getMinRate();
        availableBTC.divide(minRate, RoundingMode.HALF_DOWN).intValue();
        bidOrderCount = 4;
        return bidOrderCount;
    }

    public void orderPlaceBid(List<RankPair> rankPairList){
        int bidOrderCount = this.getAvailableBidOrderCount();
        if (bidOrderCount==0) return;
        for(RankPair rankPair : rankPairList){
            double rank = rankPair.getRank();
            if(rank>Config.getMinRankForBid()){
                Ticker ticker = rankPair.getTicker();
                CurrencyPair currencyPair = rankPair.getCurrencyPair();
                CurrencyPairMetaData currencyPairMetaData = rankPair.getCurrencyPairMetaData();
                ThreadOrderPlaceBid threadOrderPlaceBid = new ThreadOrderPlaceBid(currencyPair,ticker,rank,currencyPairMetaData,tradeService);
                this.threadOrderPlaceBids.add(threadOrderPlaceBid);
            } else{
                WaitThread(threadOrderPlaceAsks);
                System.out.println();
                return;
            }
            bidOrderCount-=1;
            if(bidOrderCount==0) {
                WaitThread(threadOrderPlaceAsks);
                System.out.println("-");
                return;
            }
        }
    }

    public void orderBidCancel(){
        // Уменьшил время ожидания до 30 секунд (на следующий прогон) если покупки не будет, то ждать нечего
        Config.setMillisecondsWaitMin();
        try {
            List<LimitOrder> openOrders = tradeService.getOpenOrders().getOpenOrders();
            for(LimitOrder limitOrder : openOrders){
                ThreadOrderCancelBid threadOrderCancelBid = new ThreadOrderCancelBid(limitOrder, tradeService);
                this.ThreadOrderCancelBids.add(threadOrderCancelBid);
            }
            WaitThread(ThreadOrderCancelBids);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static <T> void WaitThread(LinkedList<T> list)   {
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
}
