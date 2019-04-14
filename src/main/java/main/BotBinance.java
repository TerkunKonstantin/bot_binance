package main;

import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.meta.CurrencyPairMetaData;
import pairs.CurrencyPairWork;
import pairs.RankPair;
import restrictions.PairRestrictions;
import sale_block.BalanceScore;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeUnit;

class BotBinance {

    private Exchange binance;
    private Map<CurrencyPair, CurrencyPairMetaData> currencyPairs;

    BotBinance(String name, String apiKeyB, String secretKeyB){
        this.binance = ExchangeFactory.INSTANCE.createExchange(name,apiKeyB,secretKeyB);
    }

    void TakeCurrencyPairs(){
        //Получил список торговых пар
       this.currencyPairs = CurrencyPairWork.CurrencyPairGet(binance);
        //Ограничил торги только по BTC
        PairRestrictions.OnlyBTC(currencyPairs);
        //TODO вынести этот метод в отдельный класс (работать с его объектом, как это сделано для балансов)
        //Ограничил торги по парам которые мне не нравятся (убираю то, что у меня на долгом хранении)
        try {
            PairRestrictions.RemoveLongStorageDB_forPair(currencyPairs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    void Trade() {
        try {

            // Обновляю список валютных вар для работы/ранжирования их
            Map<CurrencyPair, CurrencyPairMetaData> currencyPairsForSale = CurrencyPairWork.CurrencyPairForSale(currencyPairs);

            // Создал объект по работе с балансом
            BalanceScore balanceScore = new BalanceScore(binance);

            // Расстановка ордеров на продажу
            balanceScore.orderPlaceAsk();

            // Удалил из списка монет все монеты, которые поставил на покупку
            PairRestrictions.RemovePairforSale(balanceScore,currencyPairsForSale);

            // Прошелся по списку торговых пар на покупку и создал список объектов рангов
            List<RankPair> rankPairList = RankPair.GenerateRankPairList(currencyPairsForSale,binance);

            // Рассчитал для пар ранги
            for(RankPair rankPair:rankPairList){
                rankPair.CalculateRank();
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

        }
        catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }
}
