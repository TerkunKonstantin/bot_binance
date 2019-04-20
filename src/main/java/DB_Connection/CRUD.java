package DB_Connection;

import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CRUD implements CRUD_LongStoragePair {
    private final static String USERNAME = CRUD_config.getUSERNAME();
    private final static String PASSWORD = CRUD_config.getPASSWORD();
    private final static String URL = CRUD_config.getURL();

    @Override
    public List<CurrencyPair> SelectPairs() throws SQLException {
        ArrayList<CurrencyPair> currencyPairList = new ArrayList<>();
        ProcessorDB processor = new ProcessorDB();
        Connection connection = processor.getConnection(URL, USERNAME, PASSWORD);
        String query = "select * from schema_for_test.long_storage_pairs";
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        ResultSet resultSet = preparedStatement.executeQuery();
        while (resultSet.next()) {
            String currency_1 = resultSet.getString("currency_1");
            String currency_2 = resultSet.getString("currency_2");
            CurrencyPair currencyPair = new CurrencyPair(currency_1, currency_2);
            currencyPairList.add(currencyPair);
        }
        preparedStatement.close();
        connection.close();
        return currencyPairList;
    }

    @Override
    public List<Currency> SelectCurrency() throws SQLException {
        ArrayList<Currency> currencyList = new ArrayList<>();
        ProcessorDB processor = new ProcessorDB();
        Connection connection = processor.getConnection(URL, USERNAME, PASSWORD);
        String query = "select * from schema_for_test.long_storage_pairs";
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        ResultSet resultSet = preparedStatement.executeQuery();
        while (resultSet.next()) {
            String currency_1 = resultSet.getString("currency_1");
            Currency currency = new Currency(currency_1);
            currencyList.add(currency);
        }
        preparedStatement.close();
        connection.close();
        return currencyList;
    }

    //TODO сделать реализацию вставки пары на долгое хранение
    @Override
    public boolean InsertPair(CurrencyPair currencyPair) {
        return false;
    }

    //TODO сделать реализацию удаления пары на долгое хранение
    @Override
    public boolean DeletePair(CurrencyPair currencyPair) {
        return false;
    }

}
