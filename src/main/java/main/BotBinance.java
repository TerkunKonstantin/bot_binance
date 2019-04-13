package main;

import org.knowm.xchange.Exchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.account.Balance;
import org.knowm.xchange.dto.meta.CurrencyPairMetaData;
import org.knowm.xchange.dto.meta.ExchangeMetaData;
import org.knowm.xchange.service.account.AccountService;
import org.knowm.xchange.service.marketdata.MarketDataService;
import org.knowm.xchange.service.trade.TradeService;
import pairs.CurrencyPairWork;
import pairs.RankPair;
import restrictions.PairRestrictions;
import sale_block.BalanceScore;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

import static java.util.Collections.*;

public class BotBinance {
    public static void main(String[] args) throws IOException {
        Locale.setDefault(Locale.US);
        Exchange binance = BinanceAuthorization.createExchange();

        // Получил основные сервисы
        AccountService accountService = binance.getAccountService();
        TradeService tradeService = binance.getTradeService();
        ExchangeMetaData exchangeMetaData = binance.getExchangeMetaData();
        MarketDataService marketDataService = binance.getMarketDataService();

        //Получил список торговых пар
        Map<CurrencyPair, CurrencyPairMetaData> currencyPairs = CurrencyPairWork.CurrencyPairGet(exchangeMetaData);

        //Ограничил торги только по BTC
        PairRestrictions.OnlyBTC(currencyPairs);

        //TODO вынести этот метод в отдельный класс (работать с его объектом, как это сделано для балансов)
        //Ограничил торги по парам которые мне не нравятся (убираю то, что у меня на долгом хранении)
        try {
            PairRestrictions.RemoveLongStorageDB_forPair(currencyPairs);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        while(true) {
            try {

            // Обновляю список валютных вар для работы/ранжирования их
            Map<CurrencyPair, CurrencyPairMetaData> currencyPairsForSale = CurrencyPairWork.CurrencyPairForSale(currencyPairs);

            // Получил информацию о балансе
            Map<Currency, Balance> balances = accountService.getAccountInfo().getWallet().getBalances();

            // Создал объект по работе с балансом
            BalanceScore balanceScore = new BalanceScore(balances, exchangeMetaData);

            // Расстановка ордеров на продажу
            balanceScore.orderPlaceAsk(tradeService);

            // Удалил из списка монет все монеты, которые поставил на покупку
            PairRestrictions.RemovePairforSale(balanceScore,currencyPairsForSale);

            // Прошелся по списку торговых пар на покупку и создал список объектов рангов
            List<RankPair> rankPairList = RankPair.GenerateRankPairList(currencyPairsForSale,marketDataService);

            // Рассчитал для пар ранги
            for(RankPair rankPair:rankPairList){
                rankPair.CalculateRank();
            }

            // Отсортировал по рангам
            sort(rankPairList, RankPair.Comparators.RANK);
            
            // Поставил ордера на покупку
            balanceScore.orderPlaceBid(rankPairList, tradeService);

            // Подождал время, достаточное для покупки
            Thread.sleep(Config.getMillisecondsWait());

            // Делаем отмену ордеров в пункте выше
            balanceScore.orderBidCancel(tradeService);

            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
