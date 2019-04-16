package main;

import serviceStat.StatServiceApp;

public class Main {

    public static void main(String[] args) {

        StatServiceApp controller = new StatServiceApp();
        controller.start();

        /*
        BotBinance botBinance = new BotBinance(BinanceExchange.class.getName(),Config.getApiKeyB(),Config.getSecretKeyB());
        botBinance.TakeCurrencyPairs();
        while(true){
            botBinance.Trade();


        }
         */
    }
}
