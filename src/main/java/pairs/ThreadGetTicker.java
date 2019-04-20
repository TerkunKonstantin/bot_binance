package pairs;

import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.meta.CurrencyPairMetaData;
import org.knowm.xchange.service.marketdata.MarketDataService;

import java.io.IOException;
import java.util.Map;

public class ThreadGetTicker extends Thread {

    Ticker ticker;
    private MarketDataService marketDataService;
    CurrencyPair currencyPair;
    OrderBook orderBook;

    ThreadGetTicker(Map.Entry<CurrencyPair, CurrencyPairMetaData> entry, MarketDataService marketDataService) {
        this.currencyPair = entry.getKey();
        this.marketDataService = marketDataService;
        this.start();
    }

    public void run() {
        try {
            this.ticker = marketDataService.getTicker(currencyPair);
            this.orderBook = marketDataService.getOrderBook(currencyPair);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
