package pairs;

import org.knowm.xchange.Exchange;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.meta.CurrencyPairMetaData;
import org.knowm.xchange.service.marketdata.MarketDataService;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class RankPairFabric {

    /**
     * @param currencyPairs список Торговых пар
     * @param binance переменная биржи
     * @return
     * Создаем список пар с рангами
     */
    public List<RankPair> generateRankPairList(Map<CurrencyPair, CurrencyPairMetaData> currencyPairs, Exchange binance) {
        MarketDataService marketDataService = binance.getMarketDataService();
        // Потоками получил по каждой паре список ордеров и тикет
        LinkedList<ThreadGetTicker> threadGetTickerLinkedList = new LinkedList<>();
        for (Map.Entry<CurrencyPair, CurrencyPairMetaData> entry : currencyPairs.entrySet()) {
            //TODO Есть случаи, когда возвращается пустые билеты и ордера, т.к. у нас нет пары на бирже такой, понять как их исключить и не слать лишние запросы
            ThreadGetTicker threadGetTicker = new ThreadGetTicker(entry, marketDataService);
            threadGetTickerLinkedList.add(threadGetTicker);
        }
        waitThread(threadGetTickerLinkedList);

        // Создал объекты рангов и вернул список
        List<RankPair> rankPairList = new ArrayList<>();
        for (ThreadGetTicker threadGetTicker : threadGetTickerLinkedList) {
            CurrencyPair currencyPair = threadGetTicker.currencyPair;
            CurrencyPairMetaData currencyPairMetaData = currencyPairs.get(currencyPair);
            if ((null != currencyPairMetaData) && (null != threadGetTicker.orderBook) && (null != threadGetTicker.ticker)) {
                RankPair rankPair = new RankPair(currencyPair, currencyPairMetaData, threadGetTicker.orderBook, threadGetTicker.ticker);
                rankPairList.add(rankPair);
            }
        }
        return rankPairList;
    }

    // TODO использую метод в двух местах, может его нужно вынести куда-то и сделать обращение единым, а не дублировать в классах
    private <T> void waitThread(LinkedList<T> list) {
        for (T thread : list) {
            if (((Thread) thread).isAlive()) {
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
