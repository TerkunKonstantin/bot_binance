package main;

import org.knowm.xchange.binance.BinanceExchange;

public class Main {

    public static void main(String[] args) {
        BotBinance botBinance = new BotBinance(BinanceExchange.class.getName(), Config.getApiKeyB(), Config.getSecretKeyB());
        botBinance.takeCurrencyPairs();
        while (true) {
            botBinance.Trade();
        }
    }
}
