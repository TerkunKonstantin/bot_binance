package Gui;

import main.BotBinance;
import main.Config;
import org.knowm.xchange.binance.BinanceExchange;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

class BotStarter {
    private List<StateListener> stateListenerList = new ArrayList<StateListener>();
    private List<RangePairListener> rangePairListenerList = new ArrayList<RangePairListener>();
    private BotBinance botBinance;
    private ScheduledExecutorService service;
    private ScheduledFuture scheduledFuture;

    BotStarter() {
        changeState(false);
        botBinance = new BotBinance(BinanceExchange.class.getName(), Config.getApiKeyB(), Config.getSecretKeyB());
        botBinance.addRangePairListener(message -> {
            for (RangePairListener rangePairListener : rangePairListenerList)
                rangePairListener.changeRange(message);
        });
    }

    void addStateListener(StateListener listener) {
        stateListenerList.add(listener);
    }

    void addRangePairListener(RangePairListener listener) {
        rangePairListenerList.add(listener);
    }

    void start() {
        botBinance.takeCurrencyPairs();
        service = Executors.newSingleThreadScheduledExecutor();
        scheduledFuture = service.scheduleWithFixedDelay(botBinance, 0, Config.getSecondsWait(), TimeUnit.SECONDS);
        changeState(true);
    }

    void stop() {
        if (scheduledFuture != null)
            scheduledFuture.cancel(false);
        changeState(false);
    }


    private void changeState(boolean state) {
        for (StateListener stateListener : stateListenerList)
            stateListener.changeState(state);
    }

    private void changeRange(String message) {
        for (RangePairListener rangePairListener : rangePairListenerList)
            rangePairListener.changeRange(message);
    }
}
