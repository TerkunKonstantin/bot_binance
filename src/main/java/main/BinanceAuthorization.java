package main;

import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.binance.BinanceExchange;

public class BinanceAuthorization {
    static Exchange createExchange() {
        ExchangeSpecification exSpec = new BinanceExchange().getDefaultExchangeSpecification();
        exSpec.setApiKey(Config.getApiKeyB());
        exSpec.setSecretKey(Config.getSecretKeyB());
        return ExchangeFactory.INSTANCE.createExchange(exSpec);
    }
}
