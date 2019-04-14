package sale_block;

import main.Config;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.account.Balance;
import org.knowm.xchange.dto.meta.CurrencyPairMetaData;
import org.knowm.xchange.dto.meta.ExchangeMetaData;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.dto.trade.UserTrade;
import org.knowm.xchange.service.trade.TradeService;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

public class ThreadOrderPlaceAsk extends Thread {

    private Balance balance;
    private CurrencyPairMetaData currencyPairMetaData;
    private CurrencyPair currencyPair;
    private UserTrade userTrade;
    private static BigDecimal profitPercent;
    private TradeService tradeService;
    private boolean makeTrade;


    public CurrencyPair getCurrencyPair() {
        return currencyPair;
    }

    ThreadOrderPlaceAsk(Map.Entry<Currency, Balance> entry, ExchangeMetaData exchangeMetaData, UserTrade userTrade, TradeService tradeService){
        this.currencyPair = new CurrencyPair(entry.getKey(), Config.getCurrency_for_sale());
        this.currencyPairMetaData = exchangeMetaData.getCurrencyPairs().get(currencyPair);
        this.balance = entry.getValue();
        profitPercent = Config.getPercentProfit().movePointLeft(2).add(BigDecimal.ONE);
        this.userTrade = userTrade;
        this.tradeService = tradeService;
        this.start();
    }

    public void run()
    {
        // Считаем цену продажи (с округлением вверх)
        BigDecimal priceForSale = userTrade.getPrice().multiply(profitPercent);
        priceForSale = priceForSale.setScale(currencyPairMetaData.getPriceScale(),BigDecimal.ROUND_UP);
        // Считаем стоимость в битках, будем сранивать с минимальной стоимостью в битках
        BigDecimal amount = balance.getAvailable();
        BigDecimal amountInBTC = amount.multiply(priceForSale);
        BigDecimal minNotional = Config.getMinNotional();
        // По количеству продаваемых монет делаем округление вниз до необходимого кол-ва знаков после запятой
        int amountScale = currencyPairMetaData.getMinimumAmount().scale();
        amount = amount.setScale(amountScale,BigDecimal.ROUND_DOWN);
        if((amountInBTC.compareTo(minNotional)>0)) {
            Date datePlace = userTrade.getTimestamp();
            LimitOrder ask = new LimitOrder(Order.OrderType.ASK, amount, currencyPair, null, null, priceForSale);
            System.out.println("Продаю с: " + datePlace + " Валюту " +currencyPair + " по " + priceForSale);
            try {
                tradeService.placeLimitOrder(ask);
                this.makeTrade = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
