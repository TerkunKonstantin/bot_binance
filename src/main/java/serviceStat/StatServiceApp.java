package serviceStat;

import retrofit2.*;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class StatServiceApp implements Callback<StatServiceAnsver> {

    private static final String BASE_URL = "http://128.71.219.154:8090";
    private StatServiceApi serviceApiAPI;

    public StatServiceApp (){
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        this.serviceApiAPI = retrofit.create(StatServiceApi.class);
    }


    public void start(String symbol, String price) {
        Call<StatServiceAnsver> call = serviceApiAPI.sendData(symbol, price);
        call.enqueue(this);
    }

    @Override
    public void onResponse(Call<StatServiceAnsver> call, Response<StatServiceAnsver> response) {
        if(response.isSuccessful()) {
            StatServiceAnsver answer = response.body();
            System.out.println(answer);
        } else {
            System.out.println(response.errorBody().source());
        }
    }

    @Override
    public void onFailure(Call<StatServiceAnsver> call, Throwable t) {
        t.printStackTrace();
    }
}