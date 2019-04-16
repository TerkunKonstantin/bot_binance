package serviceStat;


import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

import java.util.List;

public interface StatServiceApi {

    @GET("/queue")
    Call<List<StatServiceAnsver>> sendData(@Query("symbol") String symbol, @Query("price") String  price);
}
