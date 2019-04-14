package pairs;

import org.knowm.xchange.Exchange;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.meta.CurrencyPairMetaData;
import org.knowm.xchange.dto.meta.ExchangeMetaData;

import java.util.HashMap;
import java.util.Map;

public class CurrencyPairWork {

    public static Map<CurrencyPair, CurrencyPairMetaData> CurrencyPairGet(Exchange binance){
        ExchangeMetaData exchangeMetaData = binance.getExchangeMetaData();
        Map<CurrencyPair, CurrencyPairMetaData> currencyPairs = exchangeMetaData.getCurrencyPairs();
        Map<CurrencyPair, CurrencyPairMetaData> currencyPairsForWork = new HashMap<>(currencyPairs);
        return currencyPairsForWork;
    }

    public static Map<CurrencyPair, CurrencyPairMetaData> CurrencyPairForSale(Map<CurrencyPair, CurrencyPairMetaData> currencyPairs){
        Map<CurrencyPair, CurrencyPairMetaData> CurrencyPairForSale = new HashMap<>(currencyPairs);
        return CurrencyPairForSale;
    }
}
