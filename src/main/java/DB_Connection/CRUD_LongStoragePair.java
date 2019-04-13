package DB_Connection;

import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;

import java.sql.SQLException;
import java.util.List;

public interface CRUD_LongStoragePair {

    boolean InsertPair(CurrencyPair currencyPair) throws SQLException;

    boolean DeletePair(CurrencyPair currencyPair) throws SQLException;

    List<CurrencyPair> SelectPairs() throws SQLException;

    List<Currency> SelectCurrency() throws SQLException;

}
