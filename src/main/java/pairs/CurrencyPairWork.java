package pairs;

import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.meta.CurrencyPairMetaData;
import org.knowm.xchange.dto.meta.ExchangeMetaData;

import java.util.HashMap;
import java.util.Map;

public class CurrencyPairWork {

    public static Map<CurrencyPair, CurrencyPairMetaData> CurrencyPairGet(ExchangeMetaData exchangeMetaData){
        Map<CurrencyPair, CurrencyPairMetaData> currencyPairs = exchangeMetaData.getCurrencyPairs();
        Map<CurrencyPair, CurrencyPairMetaData> currencyPairsForWork = new HashMap();
        for (Map.Entry<CurrencyPair, CurrencyPairMetaData> entry : currencyPairs.entrySet()) {
            currencyPairsForWork.put(entry.getKey(),entry.getValue());
        }
        return currencyPairsForWork;
    }

    public static Map<CurrencyPair, CurrencyPairMetaData> CurrencyPairForSale(Map<CurrencyPair, CurrencyPairMetaData> currencyPairs){
        Map<CurrencyPair, CurrencyPairMetaData> CurrencyPairForSale = new HashMap();
        for (Map.Entry<CurrencyPair, CurrencyPairMetaData> entry : currencyPairs.entrySet()) {
            CurrencyPairForSale.put(entry.getKey(),entry.getValue());
        }
        return CurrencyPairForSale;
    }


}
