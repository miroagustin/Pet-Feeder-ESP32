package soa.L6.pet_feeder.Model;

import java.io.Serializable;
import java.util.Objects;

public class Pet implements Serializable, Comparable<Pet>
{
    private static final long serialVersionUID = 1L; // Versión del serializable

    private String name;
    private int feed_times;
    private double food_amount;
    private double eat_average;
    private String rfid_key;

    public Pet(String name,String key)
    {
        this.name = name;
        this.feed_times = 0;
        this.food_amount = 0;
        this.eat_average = 0;
        this.rfid_key = key;
    }

    public void record_meal(double ate_amount)
    {
        feed_times++;
        food_amount += ate_amount;
        eat_average = food_amount / feed_times;
    }



    @Override
    public String toString()
    {
        return "Pet{" +
                "name='" + name + '\'' +
                ", feed_times=" + feed_times +
                ", rfid=" + rfid_key +
                '}';
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pet pet = (Pet) o;
        return feed_times == pet.feed_times &&
                Double.compare(pet.food_amount, food_amount) == 0 &&
                Double.compare(pet.eat_average, eat_average) == 0 &&
                Objects.equals(name, pet.name) &&
                Objects.equals(rfid_key, pet.rfid_key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, feed_times, food_amount, eat_average);
    }

    public String getRfid_key() {
        return rfid_key;
    }

    public void setRfid_key(String rfid_key) {
        this.rfid_key = rfid_key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getFeed_times() {
        return feed_times;
    }

    public void setFeed_times(int feed_times) {
        this.feed_times = feed_times;
    }

    public double getFood_amount() {
        return food_amount;
    }

    public void setFood_amount(float food_amount) {
        this.food_amount = food_amount;
    }

    public double getEat_average() {
        return eat_average;
    }

    public void setEat_average(float eat_average) {
        this.eat_average = eat_average;
    }

    @Override
    public int compareTo(Pet o)
    {
        if(this.rfid_key.compareTo(o.rfid_key) == 0){
            return 0;
        }

        return 1;
    }
}
