package sale_block;

import DB_Connection.CRUD;
import DB_Connection.CRUD_LongStoragePair;
import Gui.RangePairListener;
import main.Config;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.binance.service.BinanceTradeHistoryParams;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.account.Balance;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.meta.CurrencyPairMetaData;
import org.knowm.xchange.dto.meta.ExchangeMetaData;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.dto.trade.UserTrade;
import org.knowm.xchange.service.trade.TradeService;
import pairs.RankPair;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.*;

public class BalanceScore {
    private Map<Currency, Balance> updateBalance;
    private ExchangeMetaData exchangeMetaData;
    private TradeService tradeService;
    private LinkedList<ThreadOrderPlaceAsk> threadOrderPlaceAsks = new LinkedList<>();
    private LinkedList<ThreadOrderPlaceBid> threadOrderPlaceBids = new LinkedList<>();
    private LinkedList<ThreadOrderCancelBid> ThreadOrderCancelBids = new LinkedList<>();
    private BigDecimal availableBTC;
    private List<RangePairListener> rangePairListenerList = new ArrayList<>();

    public void addRangePairListener(RangePairListener listener) {
        rangePairListenerList.add(listener);
    }

    private void changeRange(String message) {
        for (RangePairListener rangePairListener : rangePairListenerList)
            rangePairListener.changeRange(message);
    }

    public BalanceScore(Exchange binance) throws IOException {
        // получаю необходимые сервисы
        exchangeMetaData = binance.getExchangeMetaData();
        tradeService = binance.getTradeService();
        //меняем неизменяемую мапу на обычную
        Map<Currency, Balance> balanceMap = binance.getAccountService().getAccountInfo().getWallet().getBalances();
        updateBalance = new HashMap<>(balanceMap);
        availableBTC = updateBalance.get(Currency.BTC).getAvailable();
        // Убрал пары долгого хранения из списка продаж
        this.removeLongStorage();
        // Оставил только те пары, которые торгуются с BTC
        this.onlyBTC();
        // Посмотреть по каким парам достаточно для продажи, остальные убрать (Проверку делаю по полю minAmount)
        // TODO обработка нулпоинтерэкзепшен, если будет перед onlyBTC до этого момента (!Обязательно после onlyBTC!)
        this.enoughForSale();
    }

    public LinkedList<ThreadOrderPlaceAsk> getThreadOrderPlaceAsks() {
        return threadOrderPlaceAsks;
    }

    /**
     * Расставляем оредера на продажу
     */
    public void orderPlaceAsk() {
        // бегу по парам баланса и запускаю поток на расстановку ордера, передаю ему историю торгов
        for (Map.Entry<Currency, Balance> entry : updateBalance.entrySet()) {
            try {
                CurrencyPair currencyPair = new CurrencyPair(entry.getKey(), Config.getCurrency_for_sale());
                List<UserTrade> userTrades = tradeService.getTradeHistory(new BinanceTradeHistoryParams(currencyPair)).getUserTrades();
                for (int i = userTrades.size() - 1; i >= 0; i--) {
                    UserTrade userTrade = userTrades.get(i);
                    if (Order.OrderType.BID.equals(userTrade.getType())) {
                        ThreadOrderPlaceAsk threadOrderPlaceAsk = new ThreadOrderPlaceAsk(entry, exchangeMetaData, userTrade, tradeService);
                        threadOrderPlaceAsks.add(threadOrderPlaceAsk);
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        waitThread(threadOrderPlaceAsks);
    }

    /**
     * @return Метод возвращает кол-во возможных покупок, на которых хватает баланса
     */
    private int getAvailableBidOrderCount() {
        int bidOrderCount;
        BigDecimal minRate = Config.getMinRate();
        bidOrderCount = availableBTC.divide(minRate, RoundingMode.HALF_DOWN).intValue();
        return bidOrderCount;
        //return 4;
    }

    /**
     * @param rankPairList Расстановка ордеров на покупку, на основании рангов пар и доступного баланса
     */
    public void orderPlaceBid(List<RankPair> rankPairList) {
        int bidOrderCount = getAvailableBidOrderCount();
        if (bidOrderCount == 0) return;
        for (RankPair rankPair : rankPairList) {
            double rank = rankPair.getRank();
            if (rank > Config.getMinRankForBid()) {
                Ticker ticker = rankPair.getTicker();
                CurrencyPair currencyPair = rankPair.getCurrencyPair();
                CurrencyPairMetaData currencyPairMetaData = rankPair.getCurrencyPairMetaData();
                ThreadOrderPlaceBid threadOrderPlaceBid = new ThreadOrderPlaceBid(currencyPair, ticker, rank, currencyPairMetaData, tradeService);
                this.threadOrderPlaceBids.add(threadOrderPlaceBid);
            } else {
                waitThread(threadOrderPlaceAsks);
                System.out.println();
                return;
            }
            bidOrderCount -= 1;
            if (bidOrderCount == 0) {
                waitThread(threadOrderPlaceAsks);
                System.out.println("-");
                return;
            }
        }
    }

    /**
     * Отмена покупки монет. То, что мы не успели купить, нужно отменить.
     * Новое ранирование выберет новые пары.
     */
    public void orderBidCancel() {
        // Уменьшил время ожидания до 30 секунд (на следующий прогон) если покупки не будет, то ждать нечего
        Config.setSecondsWait(Config.getSecondsWaitMin());
        try {
            List<LimitOrder> openOrders = tradeService.getOpenOrders().getOpenOrders();
            for (LimitOrder limitOrder : openOrders) {
                ThreadOrderCancelBid threadOrderCancelBid = new ThreadOrderCancelBid(limitOrder, tradeService);
                this.ThreadOrderCancelBids.add(threadOrderCancelBid);
            }
            waitThread(ThreadOrderCancelBids);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * @param list список
     * @param <T>  Метод используется для ожидаения потоков из списка.
     *             Ждем завершения последнего и идем дальше.
     */
    private <T> void waitThread(LinkedList<T> list) {
        for (T thread : list) {
            if (((Thread) thread).isAlive()) {
                try {
                    ((Thread) thread).join();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }


    /**
     * Метод удаляет пары долгого хранения
     */
    private void removeLongStorage() {
        CRUD_LongStoragePair impl = new CRUD();
        List<Currency> currencyList = null;
        try {
            currencyList = impl.SelectCurrency();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        assert currencyList != null;
        updateBalance.keySet().removeAll(currencyList);
    }


    /**
     * Метод оставляет торговые пары только с BTC
     */
    private void onlyBTC() {
        List<Currency> currencyList = new ArrayList<>();
        for (Map.Entry<Currency, Balance> entry : updateBalance.entrySet()) {
            // Если по паре с BTC торговая информация отсутствует, то мы выкидываем пару
            Currency currency = entry.getKey();
            Map<CurrencyPair, CurrencyPairMetaData> currencyPairs = exchangeMetaData.getCurrencyPairs();
            CurrencyPair currencyPair = new CurrencyPair(currency, Config.getCurrency_for_sale());
            CurrencyPairMetaData currencyPairMetaData = currencyPairs.get(currencyPair);
            if (currencyPairMetaData == null) {
                currencyList.add(currency);
            }
        }
        updateBalance.keySet().removeAll(currencyList);
    }


    /**
     * Метод убирает из списка пар баланса те, по которым не достаточно средств для продажи.
     * Их мы можем докупать.
     */
    private void enoughForSale() {
        List<Currency> currencyList = new ArrayList<>();
        for (Map.Entry<Currency, Balance> entry : updateBalance.entrySet()) {
            // Если по паре недостаточно средств, то выкидываем ее
            Currency currency = entry.getKey();
            Map<CurrencyPair, CurrencyPairMetaData> currencyPairs = exchangeMetaData.getCurrencyPairs();
            CurrencyPair currencyPair = new CurrencyPair(currency, Config.getCurrency_for_sale());
            CurrencyPairMetaData currencyPairMetaData = currencyPairs.get(currencyPair);
            BigDecimal available = entry.getValue().getAvailable();
            BigDecimal minAmount = currencyPairMetaData.getMinimumAmount();
            if (available.compareTo(minAmount) < 0) {
                currencyList.add(currency);
            }
        }
        updateBalance.keySet().removeAll(currencyList);
    }


    /**
     * @param currencyPairs Удаляем из списка монеты, которые мы продаем (докупать их не нужно)
     */
    public void removePairForSale(Map<CurrencyPair, CurrencyPairMetaData> currencyPairs) {
        // Получил список продаваемых монет на этом круге цикла
        LinkedList<ThreadOrderPlaceAsk> listThreadTicker = getThreadOrderPlaceAsks();
        for (ThreadOrderPlaceAsk threadOrderPlaceAsk : listThreadTicker) {
            currencyPairs.keySet().remove(threadOrderPlaceAsk.getCurrencyPair());
        }
    }


    private String messageForRangPair(){
        StringBuilder message = new StringBuilder();
        for (ThreadOrderPlaceBid threadOrderPlaceBid: threadOrderPlaceBids){
            message.append(threadOrderPlaceBid.messageInfo).append("\n");
        }

        for (ThreadOrderPlaceAsk threadOrderPlaceAsk: threadOrderPlaceAsks){
            message.append(threadOrderPlaceAsk.messageInfo).append("\n");
        }

        return message.toString();
    }

    public void printGuiMessage(){
        changeRange(messageForRangPair());
    }


}
