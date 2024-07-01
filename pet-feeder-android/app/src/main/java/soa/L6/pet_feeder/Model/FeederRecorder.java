package soa.L6.pet_feeder.Model;

import android.content.Context;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FeederRecorder {

    private List<Food> foodList;
    private String filename;
    public FeederRecorder(String filename){
        foodList = new ArrayList<>();
        this.filename = filename;
    }

    public void saveFoodToFile(Context context) {
        FileOutputStream fos = null;
        ObjectOutputStream oos = null;
        try {
            fos = context.openFileOutput(filename, Context.MODE_PRIVATE);
            oos = new ObjectOutputStream(fos);
            oos.writeObject(foodList);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (oos != null) {
                try {
                    oos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Método para cargar una lista de Pet desde un archivo
    public void loadFoodsFromFile(Context context) {
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        List<Food> foods = new ArrayList<>();
        try {
            fis = context.openFileInput(filename);
            ois = new ObjectInputStream(fis);
            foods = (List<Food>) ois.readObject();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        foodList = foods;
    }

    public void addFoodToList(Food newFood){
        if (!foodList.contains(newFood)) {
            foodList.add(newFood);
        }
    }

    public void clearFoodList(){
        foodList.clear();
    }

    public List<Food> getFoodList() {
        return foodList;
    }

    public void setFoodList(List<Food> foodList) {
        this.foodList = foodList;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public boolean exists(Food food)
    {
        return foodList.stream().anyMatch(x -> x.getHour().compareTo(food.getHour()) == 0);
    }

    public void updateFood(Food foodUpdate)
    {
        foodList.replaceAll(x -> x.compareTo(foodUpdate) > 0 ? foodUpdate : x);
    }

}
