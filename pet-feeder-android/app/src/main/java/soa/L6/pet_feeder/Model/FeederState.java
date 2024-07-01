package soa.L6.pet_feeder.Model;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import soa.L6.pet_feeder.Utils.PetFeederConstants;

public class FeederState {

    private String nextMealTime;
    private double foodAmount;
    private boolean refillNeed;
    private boolean clearNeed;
    private EstadoEmbebido estado;
    //private List<Food> alimentaciones = new ArrayList<>();
    private FeederRecorder feederRecorder = new FeederRecorder(PetFeederConstants.FILE_NAME_FOODS);
    private Context context;

    public FeederState(Context context){
        this.context = context;
        feederRecorder.loadFoodsFromFile(this.context);
        Food food = getNextFood();
        nextMealTime = food.getHour();
        foodAmount = food.getFood_amount();
        refillNeed = false;
        clearNeed = false;
    }
    public FeederState(String nextMealTime, double foodAmount, boolean refillNeed, boolean clearNeed) {
        this.nextMealTime = nextMealTime;
        this.foodAmount = foodAmount;
        this.refillNeed = refillNeed;
        this.clearNeed = clearNeed;
    }
    @Override
    public String toString() {
        return "FeederState{" +
                "nextMealTime='" + nextMealTime + '\'' +
                ", foodAmount=" + foodAmount +
                ", refillNeed=" + refillNeed +
                ", clearNeed=" + clearNeed +
                ", estado=" + estado +
                ", alimentaciones=" + feederRecorder.getFoodList() +
                '}';
    }
    public Food getNextFood() {
        Food res = new Food("",0.0);

        if(!feederRecorder.getFoodList().isEmpty())
        {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
            String actual = sdf.format(new Date());

            Food foodActual = new Food(actual,0.0);
            List<Food> foodList = feederRecorder.getFoodList().stream().filter(x -> x.compareTo(foodActual) > 0 ).sorted(Comparator.comparing(Food::getHour)).collect(Collectors.toList());
            res = foodList.isEmpty() ? feederRecorder.getFoodList().get(0) : foodList.get(0);
        }

        return res;
    }
    public String getNextMealTime() {
        return getNextFood().getHour();
    }
    public void setNextMealTime(String nextMealTime) {
        this.nextMealTime = nextMealTime;
    }
    public double getFoodAmount() {
        return getNextFood().getFood_amount();
    }
    public void setFoodAmount(double foodAmount) {
        this.foodAmount = foodAmount;
    }
    public boolean isRefillNeed() {
        return refillNeed;
    }
    public void setRefillNeed(boolean refillNeed) {
        this.refillNeed = refillNeed;
    }
    public boolean isClearNeed() {
        return clearNeed;
    }
    public void setClearNeed(boolean clearNeed) {
        this.clearNeed = clearNeed;
    }
    public void AddAlimentacion(String horario, double cantComida) {
        feederRecorder.addFoodToList(new Food(horario,cantComida));
        feederRecorder.saveFoodToFile(context);
    }
    public void UpdateEstado(String message) {
        estado = EstadoEmbebido.fromString(message);
        if (Objects.equals(estado.getEstadoActual(), PetFeederConstants.ESTADO_RENOVAR_COMIDA))
            clearNeed = true;
        if (Objects.equals(estado.getEstadoActual(), PetFeederConstants.ESTADO_PEDIR_RECARGA))
            refillNeed = true;
        if(Objects.equals(estado.getEstadoActual(), PetFeederConstants.ESTADO_ESPERA)) {
            refillNeed = false;
            clearNeed = false;
        }
        Log.d(FeederState.class.getName(), "Estado Updateado " + estado);
    }

    public void clearFoodList()
    {
        feederRecorder.clearFoodList();
        feederRecorder.saveFoodToFile(context);
    }
    public FeederRecorder getFeederRecorder()
    {
        return feederRecorder;
    }
}
