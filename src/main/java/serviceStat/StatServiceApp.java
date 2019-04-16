package serviceStat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import retrofit2.*;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;


import java.util.List;

public class StatServiceApp implements Callback<List<StatServiceAnsver>> {

    static final String BASE_URL = "http://128.71.219.154:8090";

    public void start() {
        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        StatServiceApi serviceApiAPI = retrofit.create(StatServiceApi.class);

        Call<List<StatServiceAnsver>> call = serviceApiAPI.sendData("BTCETH","12.123");
        call.enqueue(this);

    }

    @Override
    public void onResponse(Call<List<StatServiceAnsver>> call, Response<List<StatServiceAnsver>> response) {
        if(response.isSuccessful()) {
            List<StatServiceAnsver> ansversList = response.body();
            ansversList.forEach(change -> System.out.println(change.subject));
        } else {
            System.out.println(response.errorBody());
        }
    }

    @Override
    public void onFailure(Call<List<StatServiceAnsver>> call, Throwable t) {
        t.printStackTrace();
    }


}