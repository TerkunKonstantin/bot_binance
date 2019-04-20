package pairs;

import main.ConfigIndexParams;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.meta.CurrencyPairMetaData;
import org.knowm.xchange.dto.trade.LimitOrder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class RankPair {
    private CurrencyPair currencyPair;
    private CurrencyPairMetaData currencyPairMetaData;
    private OrderBook orderBook;
    private Ticker ticker;
    private double rank;
    // Будем хранить значения рассчитанных индексов, чтобы понимать из чего сложился ранг для пары
    private double volumeIndex;
    private double askBidDifferenceIndex;
    private double PositionIndex;


    RankPair(CurrencyPair currencyPair, CurrencyPairMetaData currencyPairMetaData, OrderBook orderBook, Ticker ticker) {
        this.currencyPair = currencyPair;
        this.currencyPairMetaData = currencyPairMetaData;
        this.orderBook = orderBook;
        this.ticker = ticker;
        this.rank = 1;
    }


    /**
     * Расчет ранга для пары
     */
    public void calculateRank() {
        if (ConfigIndexParams.getVolumeIndexActivity()) calculateVolumeIndex();
        if (ConfigIndexParams.getAskBidDifferenceIndexActivity()) calculateAskBidDifferenceIndex();
        if (ConfigIndexParams.getPositionIndexActivity()) calculatePositionIndex();
        // индекс доминирования биткоина - хочу тянуть откуда-то (хорошая штука), писать в БД и считать его движение вверх или вниз
    }

    /**
     * Расчет индекса относительного положения на графике.
     * Чем ниже, тем индекс ближе к 1
     * Чем выше, тем индекс ближе к 0
     */
    private void calculatePositionIndex() {
        if (rank <= 0) return;
        BigDecimal high = ticker.getHigh();
        BigDecimal low = ticker.getLow();
        BigDecimal last = ticker.getLast();
        if ((low.compareTo(BigDecimal.ZERO) > 0)) {
            double PositionIndexPair = high.subtract(last).divide(high.subtract(low), BigDecimal.ROUND_HALF_EVEN).doubleValue();
            rank = rank * PositionIndexPair;
            PositionIndex = PositionIndexPair;
        } else {
            double PositionIndexPair = 0;
            rank = rank * PositionIndexPair;
            PositionIndex = PositionIndexPair;
        }
    }

    /**
     * Расчет индекса разницы цены на покупку и продажу.
     * Чем больше процент разницы, тем выше индекс
     */
    private void calculateAskBidDifferenceIndex() {
        if (rank <= 0) return;
        BigDecimal ask = ticker.getAsk();
        BigDecimal bid = ticker.getBid();
        Integer priceScale = currencyPairMetaData.getPriceScale();
        BigDecimal step = BigDecimal.ONE.movePointLeft(priceScale);
        ask = ask.subtract(step);
        bid = bid.add(step);
        BigDecimal askBidDifferenceIndexBD = ask.divide(bid, RoundingMode.HALF_UP).multiply(new BigDecimal("100")).subtract(new BigDecimal("100"));
        this.askBidDifferenceIndex = askBidDifferenceIndexBD.doubleValue();
        this.rank = rank * askBidDifferenceIndex;
    }

    /**
     * Расчет индекса объема ордеров в стаканах.
     * Чем больше у нас объем ордеров на покупку, тем выше индекс
     */
    private void calculateVolumeIndex() {
        if (rank <= 0) return;
        BigDecimal limitPercentAsk = ConfigIndexParams.getVolumeIndexLimitPercent().movePointLeft(2).add(BigDecimal.ONE);
        BigDecimal limitPercentBid = BigDecimal.ONE.subtract(ConfigIndexParams.getVolumeIndexLimitPercent().movePointLeft(2));
        List<LimitOrder> asks = orderBook.getAsks();
        BigDecimal ask = ticker.getAsk();
        BigDecimal limitAskPrice = ask.multiply(limitPercentAsk);
        List<LimitOrder> bids = orderBook.getBids();
        BigDecimal bid = ticker.getBid();
        BigDecimal limitBidPrice = bid.multiply(limitPercentBid);
        BigDecimal sumOriginalAmountAsk = BigDecimal.ZERO;
        BigDecimal sumOriginalAmountBid = BigDecimal.ZERO;

        for (LimitOrder limitOrder : asks) {
            BigDecimal limitPrice = limitOrder.getLimitPrice();
            if (limitPrice.compareTo(limitAskPrice) > 0) break;
            BigDecimal originalAmountAsk = limitOrder.getOriginalAmount();
            sumOriginalAmountAsk = sumOriginalAmountAsk.add(originalAmountAsk);
        }

        for (LimitOrder limitOrder : bids) {
            BigDecimal limitPrice = limitOrder.getLimitPrice();
            if (limitPrice.compareTo(limitBidPrice) < 0) break;
            BigDecimal originalAmountBid = limitOrder.getOriginalAmount();
            sumOriginalAmountBid = sumOriginalAmountBid.add(originalAmountBid);
        }

        if (sumOriginalAmountAsk.compareTo(BigDecimal.ZERO) > 0) {
            double volumeIndexPair = sumOriginalAmountBid.divide(sumOriginalAmountAsk, 1, RoundingMode.HALF_UP).doubleValue();
            rank = rank * volumeIndexPair;
            volumeIndex = volumeIndexPair;
        } else {
            double volumeIndexPair = 0;
            rank = rank * volumeIndexPair;
            volumeIndex = volumeIndexPair;
        }
    }


    // TODO Реализацию спер и сильно не вникал - надо прочитать статью про сортировку в джава, как еще можно реализовать и т.д.
    public static class Comparators {
        static public Comparator<RankPair> RANK = (RankPair o1, RankPair o2) ->
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
