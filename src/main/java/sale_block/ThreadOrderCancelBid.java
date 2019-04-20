package sale_block;

import org.knowm.xchange.binance.service.BinanceCancelOrderParams;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.service.trade.TradeService;

import java.io.IOException;

public class ThreadOrderCancelBid extends Thread {

    private LimitOrder limitOrder;
    private TradeService tradeService;

    ThreadOrderCancelBid(LimitOrder limitOrder, TradeService tradeService) {
        this.limitOrder = limitOrder;
        this.tradeService = tradeService;
        if (limitOrder.getType().equals(Order.OrderType.BID)) this.start();
    }

    public void run() {
        BinanceCancelOrderParams binanceCancelParams = new BinanceCancelOrderParams(limitOrder.getCurrencyPair(), limitOrder.getId());
        try {
            tradeService.cancelOrder(binanceCancelParams);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
