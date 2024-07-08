package soa.L6.pet_feeder.Model;

import android.content.Context;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class PetRecorder
{

    private List<Pet> petList;
    private String filename;
    public PetRecorder(String filename)
    {
        petList = new ArrayList<>();
        this.filename = filename;
    }

    public void savePetsToFile(Context context)
    {
        FileOutputStream fos = null;
        ObjectOutputStream oos = null;
        try
        {
            fos = context.openFileOutput(filename, Context.MODE_PRIVATE);
            oos = new ObjectOutputStream(fos);
            oos.writeObject(petList);
        } catch (IOException e)
        {
            e.printStackTrace();
        } finally
        {
            if (oos != null)
            {
                try
                {
                    oos.close();
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
            if (fos != null)
            {
                try
                {
                    fos.close();
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    // Método para cargar una lista de Pet desde un archivo
    public void loadPetsFromFile(Context context)
    {
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        List<Pet> pets = new ArrayList<>();
        try
        {
            fis = context.openFileInput(filename);
            ois = new ObjectInputStream(fis);
            pets = (List<Pet>) ois.readObject();
        } catch (FileNotFoundException e)
        {
            e.printStackTrace();
        } catch (IOException | ClassNotFoundException e)
        {
            e.printStackTrace();
        } finally
        {
            if (ois != null)
            {
                try
                {
                    ois.close();
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
            if (fis != null)
            {
                try
                {
                    fis.close();
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
        petList = pets;
    }

    public void addPetToList(Pet newPet)
    {
        if (!petList.contains(newPet))
        {
            petList.add(newPet);
        }
    }

    public void updatePet(Pet petUpdate)
    {
        petList.replaceAll(x -> x.compareTo(petUpdate) == 0 ? petUpdate : x);
    }

    public void clearPetList(){
        petList.clear();
    }

    public List<Pet> getPetList() {
        return petList;
    }

    public void setPetList(List<Pet> petList) {
        this.petList = petList;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public boolean exists(Pet pet)
    {
        return petList.stream().anyMatch(x -> x.compareTo(pet) == 0);
    }

}
