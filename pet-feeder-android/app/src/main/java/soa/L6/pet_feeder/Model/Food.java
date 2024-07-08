package soa.L6.pet_feeder.Model;

import java.io.Serializable;
import java.util.Objects;

public class Food implements Serializable, Comparable<Food>
{
    private static final long serialVersionUID = 1L; // Versión del serializable

    private String hour;
    private double food_amount;

    public Food(String hour, Double food_amount)
    {
        this.hour = hour;
        this.food_amount = food_amount;
    }


    @Override
    public String toString()
    {
        return "Food{" +
                "hour='" + hour + '\'' +
                ", food_amount=" + food_amount +
                '}';
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Food food = (Food) o;
        return hour.compareTo(food.hour) == 0 &&
                Double.compare(food.food_amount, food_amount) == 0;
    }

    @Override
    public int compareTo(Food food)
    {
        // Convertimos los horarios a LocalTime para poder compararlos
        java.time.LocalTime thisTime = java.time.LocalTime.parse(this.hour);
        java.time.LocalTime otraTime = java.time.LocalTime.parse(food.getHour());

        return thisTime.compareTo(otraTime);
    }


    public String getHour() {
        return hour;
    }

    public void setHour(String name) {
        this.hour = hour;
    }

    public double getFood_amount() {
        return food_amount;
    }

    public void setFood_amount(double food_amount) {
        this.food_amount = food_amount;
    }

}
