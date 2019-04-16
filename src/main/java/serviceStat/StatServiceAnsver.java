package serviceStat;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

public class StatServiceAnsver {

    @SerializedName("symbol")
    @Expose
    private String symbol;
    @SerializedName("price")
    @Expose
    private BigDecimal price;

    @Override
    public String toString() {
        return "StatServiceAnsver{" +
                "symbol='" + symbol + '\'' +
                ", price=" + price +
                ", timestamp=" + timestamp +
                '}';
    }

    @SerializedName("timestamp")
    @Expose
    private long timestamp;

}
