package sale_block;

import main.Config;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.meta.CurrencyPairMetaData;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.service.trade.TradeService;
import serviceStat.StatServiceApp;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

public class ThreadOrderPlaceBid extends Thread {
    private CurrencyPair currencyPair;
    private Ticker ticker;
    private TradeService tradeService;
    private CurrencyPairMetaData currencyPairMetaData;
    private double rank;
    private boolean makeTrade;
    String messageInfo;
    //private static StatServiceApp controller = new StatServiceApp();


    ThreadOrderPlaceBid(CurrencyPair currencyPair, Ticker ticker, double rank, CurrencyPairMetaData currencyPairMetaData, TradeService tradeService) {
        this.currencyPair = currencyPair;
        this.ticker = ticker;
        this.tradeService = tradeService;
        this.currencyPairMetaData = currencyPairMetaData;
        this.rank = rank;
        this.start();
    }


    public void run() {
        Date date = new Date();
        BigDecimal priceForBuy;
        BigDecimal bidWithStep;
       // BigDecimal amount = Config.getMinRate();

        BigDecimal bid = ticker.getBid();
        Integer priceScale = currencyPairMetaData.getPriceScale();
        BigDecimal step = BigDecimal.ONE.movePointLeft(priceScale);
        bidWithStep = bid.add(step);
        BigDecimal askBidDifferenceIndexBD = bidWithStep.divide(bid, RoundingMode.HALF_UP).multiply(new BigDecimal("100")).subtract(new BigDecimal("100"));
        if (askBidDifferenceIndexBD.compareTo(Config.getMaxLostProfitFromOrderStep()) > 0) {
            priceForBuy = bid;
        } else {
            priceForBuy = bidWithStep;
        }
        priceForBuy = priceForBuy.setScale(currencyPairMetaData.getPriceScale(), BigDecimal.ROUND_UP);
        BigDecimal amount = Config.getMinRate().divide(priceForBuy,currencyPairMetaData.getMinimumAmount().scale(),BigDecimal.ROUND_DOWN);
        LimitOrder bidOrder = new LimitOrder(Order.OrderType.BID, amount, currencyPair, null, null, priceForBuy);
        messageInfo = "Покупаю с: " + date + " Валюту " +currencyPair + " по " + priceForBuy + " с рейтингом " + rank;
        System.out.println(messageInfo);
        //controller.start(currencyPair.base.toString() + currencyPair.counter.toString(), priceForBuy.toString());

        try {
            tradeService.placeLimitOrder(bidOrder);
            this.makeTrade = true;
            // Увеличил время ожидания до 2 минут (на следующий прогон) надо ж продать
            Config.setSecondsWait(Config.getSecondsWaitMax());

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}

