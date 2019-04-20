package main;

import DB_Connection.CRUD;
import DB_Connection.CRUD_LongStoragePair;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.meta.CurrencyPairMetaData;
import org.knowm.xchange.dto.meta.ExchangeMetaData;
import pairs.RankPair;
import pairs.RankPairFabric;
import sale_block.BalanceScore;
import sale_block.ThreadOrderPlaceAsk;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeUnit;

class BotBinance {

    private Exchange binance;
    private Map<CurrencyPair, CurrencyPairMetaData> currencyPairs;

    BotBinance(String name, String apiKeyB, String secretKeyB) {
        binance = ExchangeFactory.INSTANCE.createExchange(name, apiKeyB, secretKeyB);
    }

    /**
     * Метод получает пары с биржи. Оставляет только торгуемые с BTC. Убирает пары долгого хранения.
     */
    void takeCurrencyPairs() {
        //Получил список торговых пар
        ExchangeMetaData exchangeMetaData = binance.getExchangeMetaData();
        Map<CurrencyPair, CurrencyPairMetaData> currencyPairMetaDataMap = exchangeMetaData.getCurrencyPairs();
        currencyPairs = new HashMap<>(currencyPairMetaDataMap);
        this.onlyBTC();
        //Ограничил торги по парам которые мне не нравятся (убираю то, что у меня на долгом хранении)
        try {
            this.removeLongStorageDBForPair();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Метод реализует функцию торговли по следующему алгоритму:
     * Получение баланса
     * Продажа
     * Ранжирование пар
     * Попкупка
     */
    void Trade() {
        try {

            // Обновляю список валютных вар для работы/ранжирования их
            Map<CurrencyPair, CurrencyPairMetaData> currencyPairsForSale = new HashMap<>(currencyPairs);

            // Создал объект по работе с балансом
            BalanceScore balanceScore = new BalanceScore(binance);

            // Расстановка ордеров на продажу
            balanceScore.orderPlaceAsk();

            // Удалил из списка монет все монеты, которые поставил на покупку
            balanceScore.removePairForSale(currencyPairsForSale);

            LinkedList<ThreadOrderPlaceAsk> listThreadTicker = balanceScore.getThreadOrderPlaceAsks();
            for (ThreadOrderPlaceAsk threadOrderPlaceAsk : listThreadTicker) {
                currencyPairs.keySet().remove(threadOrderPlaceAsk.getCurrencyPair());
            }

            // Прошелся по списку торговых пар на покупку и создал список объектов рангов
            RankPairFabric rankPairFabric = new RankPairFabric();
            List<RankPair> rankPairList = rankPairFabric.generateRankPairList(currencyPairsForSale, binance);

            // Рассчитал для пар ранги
            for (RankPair rankPair : rankPairList) {
                rankPair.calculateRank();
            }

            // Отсортировал по рангам
            rankPairList.sort(RankPair.Comparators.RANK);

            // Поставил ордера на покупку
            balanceScore.orderPlaceBid(rankPairList);

            // Подождал время, достаточное для покупки
            long millisecondsWait = TimeUnit.SECONDS.toMillis(Config.getSecondsWait());
            Thread.sleep(millisecondsWait);

            // Делаем отмену ордеров в пункте выше
            balanceScore.orderBidCancel();

        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Метод оставляет только пары торгуемые с BTC
     */
    private void onlyBTC() {
        Iterator<Map.Entry<CurrencyPair, CurrencyPairMetaData>> it = currencyPairs.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<CurrencyPair, CurrencyPairMetaData> item;
            item = it.next();
            if (!(item.getKey().counter.equals(Currency.BTC))) {
                it.remove();
            }
        }
    }

    /**
     * @throws SQLException Метод убирает пары долгого хранения (то что держу "в долгую")
     */
    private void removeLongStorageDBForPair() throws SQLException {
        CRUD_LongStoragePair impl = new CRUD();
        List<CurrencyPair> currencyPairList = impl.SelectPairs();
        for (CurrencyPair currencyPair : currencyPairList) {
            currencyPairs.entrySet().removeIf(e -> currencyPair.equals(e.getKey()));
        }
    }
}
