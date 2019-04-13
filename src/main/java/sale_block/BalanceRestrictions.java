package sale_block;

import main.Config;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.account.Balance;
import org.knowm.xchange.dto.meta.CurrencyPairMetaData;
import restrictions.PairRestrictions;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BalanceRestrictions {

    public static BalanceScore RemoveLongStorage(BalanceScore balanceScore){
        try {
            balanceScore.updateBalance = PairRestrictions.RemoveLongStorageDB_forBalance(balanceScore.updateBalance);
        } catch (SQLException e) {
            balanceScore.updateBalance = new HashMap();
            e.printStackTrace();
        }
        return balanceScore;
    }

    public static BalanceScore  OnlyBTC(BalanceScore balanceScore){
        List<Currency> currencyList = new ArrayList();
        for (Map.Entry<Currency, Balance> entry : balanceScore.updateBalance.entrySet()) {
            // Если по паре с BTC торговая информация отсутствует, то мы выкидываем пару
            Currency currency = entry.getKey();
            Map<CurrencyPair, CurrencyPairMetaData> currencyPairs = balanceScore.exchangeMetaData.getCurrencyPairs();
            CurrencyPair currencyPair = new CurrencyPair(currency, Config.getCurrency_for_sale());
            CurrencyPairMetaData currencyPairMetaData = currencyPairs.get(currencyPair);
            if (currencyPairMetaData == null) {
                currencyList.add(currency);
            }
        }
        balanceScore.updateBalance.keySet().removeAll(currencyList);
        return balanceScore;
    }



    public static BalanceScore EnoughForSale(BalanceScore balanceScore){
        List<Currency> currencyList = new ArrayList();
        for (Map.Entry<Currency, Balance> entry : balanceScore.updateBalance.entrySet()) {
            // Если по паре недостаточно средств, то выкидываем ее
            Currency currency = entry.getKey();
            Map<CurrencyPair, CurrencyPairMetaData> currencyPairs = balanceScore.exchangeMetaData.getCurrencyPairs();
            CurrencyPair currencyPair = new CurrencyPair(currency, Config.getCurrency_for_sale());
            CurrencyPairMetaData currencyPairMetaData = currencyPairs.get(currencyPair);
            BigDecimal available = entry.getValue().getAvailable();
            BigDecimal minAmount = currencyPairMetaData.getMinimumAmount();
            if(available.compareTo(minAmount)<0){
                currencyList.add(currency);
            }

        }
        balanceScore.updateBalance.keySet().removeAll(currencyList);
        return balanceScore;
    }



}
