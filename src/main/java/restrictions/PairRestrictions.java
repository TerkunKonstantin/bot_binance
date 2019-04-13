package restrictions;

import DB_Connection.CRUD_LongStoragePair;
import DB_Connection.CRUD;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.account.Balance;
import org.knowm.xchange.dto.meta.CurrencyPairMetaData;
import sale_block.BalanceScore;
import sale_block.ThreadOrderPlaceAsk;

import java.sql.SQLException;
import java.util.*;

public class PairRestrictions {

    private static CRUD_LongStoragePair impl = new CRUD();

    public static Map<CurrencyPair, CurrencyPairMetaData> OnlyBTC(Map<CurrencyPair, CurrencyPairMetaData> currencyPairs){
        Iterator it = currencyPairs.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry<CurrencyPair, CurrencyPairMetaData> item = (Map.Entry<CurrencyPair, CurrencyPairMetaData>) it.next();
            if (!(item.getKey().counter.equals(Currency.BTC))) {
                it.remove();
            }
        }
         return currencyPairs;
    }


    public static Map<CurrencyPair, CurrencyPairMetaData> RemoveLongStorageDB_forPair(Map<CurrencyPair, CurrencyPairMetaData> currencyPairs) throws SQLException {
        List<CurrencyPair> currencyPairList = impl.SelectPairs();
        for(CurrencyPair currencyPair:currencyPairList){
            currencyPairs.entrySet().removeIf(e -> currencyPair.equals(e.getKey()));
        }
        return currencyPairs;
    }


    public static Map<Currency, Balance> RemoveLongStorageDB_forBalance(Map<Currency, Balance> currencyBalance) throws SQLException {
        List<Currency> currencyList = impl.SelectCurrency();
        currencyBalance.keySet().removeAll(currencyList);
        return currencyBalance;
    }

    public static Map<CurrencyPair, CurrencyPairMetaData> RemovePairforSale(BalanceScore balanceScore, Map<CurrencyPair, CurrencyPairMetaData> currencyPairs) {
        // Получил список продаваемых монет на этом круге цикла
        LinkedList<ThreadOrderPlaceAsk> listThreadTicker = balanceScore.getThreadOrderPlaceAsks();
        for(ThreadOrderPlaceAsk threadOrderPlaceAsk: listThreadTicker){
           currencyPairs.keySet().remove(threadOrderPlaceAsk.getCurrencyPair());
       }
        return currencyPairs;
    }
}
