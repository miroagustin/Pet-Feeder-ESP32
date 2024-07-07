package soa.L6.pet_feeder.ui.dashboard;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.fragment.app.Fragment;

import androidx.lifecycle.ViewModelProvider;

import soa.L6.pet_feeder.Activities.MainActivity;
import soa.L6.pet_feeder.Model.Pet;
import soa.L6.pet_feeder.Model.PetRecorder;
import soa.L6.pet_feeder.R;
import soa.L6.pet_feeder.databinding.FragmentDashboardBinding;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private AlertDialog dialog;
    MainActivity mainActivity;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        DashboardViewModel dashboardViewModel =
                new ViewModelProvider(this).get(DashboardViewModel.class);

        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        mainActivity = (MainActivity) getActivity();
        mainActivity.dashboardFragment = this;
        LinearLayout containerLayout = root.findViewById(R.id.contenedor_linear);

        PetRecorder petRecorder = mainActivity.petRecorder;

        for (Pet pet : petRecorder.getPetList()) {
            addPetToContainer(containerLayout, pet);
        }
        return root;
    }

    private static final int PADDING_LEFT = 20;
    private static final int PADDING_TOP = 10;
    private static final int PADDING_RIGHT = 0;
    private static final int PADDING_BOTTOM = 0;
    private static final int PADDING_PANEL = 16;
    public LinearLayout findPetCard(ViewGroup containerLayout, Pet pet)
    {
        // Verificar si ya existe una tarjeta con el mismo RFID
        for (int i = 0; i < containerLayout.getChildCount(); i++) {
            View child = containerLayout.getChildAt(i);
            if (child instanceof LinearLayout) {
                LinearLayout petContainer = (LinearLayout) child;
                TextView rfidTextView = (TextView) petContainer.getChildAt(1); // Asumiendo que el TextView del RFID es el segundo hijo

                if (rfidTextView.getText().toString().equals(pet.getRfid_key())) {
                    // Actualizar la tarjeta existente
                    return petContainer;
                }
            }
        }
        return null;
    }
    public void addPetCard(Pet pet)
    {
        LinearLayout containerLayout = binding.getRoot().findViewById(R.id.contenedor_linear);
        LinearLayout petContainer = findPetCard(containerLayout,pet);
        if(petContainer == null)
        {
            addPetToContainer(containerLayout,pet);
        } else {
            updatePetCard(petContainer,pet);
        }
    }
    private void updatePetCard(LinearLayout petContainer, Pet pet) {
        // Asumiendo que los TextViews están en el orden: Nombre, RFID, Cantidad de Comidas, Cantidad de Comida, Promedio Ingerido
        ((TextView) petContainer.getChildAt(0)).setText("Nombre: " + pet.getName());
        ((TextView) petContainer.getChildAt(1)).setText(pet.getRfid_key());
        ((TextView) petContainer.getChildAt(2)).setText("Cantidad de Comidas: " + pet.getFeed_times());
        ((TextView) petContainer.getChildAt(3)).setText("Cantidad de comida: " + pet.getFood_amount());
        ((TextView) petContainer.getChildAt(4)).setText("Promedio ingerido: " + String.format("%.2f", pet.getEat_average()));
    }
    private void addPetToContainer(ViewGroup container, Pet pet) {
        // Crear un contenedor para el Pet
        LinearLayout petContainer = new LinearLayout(getContext());
        petContainer.setOnClickListener(v -> editPetDialog(pet));

        petContainer.setOrientation(LinearLayout.VERTICAL);
        petContainer.setPadding(PADDING_PANEL, PADDING_PANEL, PADDING_PANEL, PADDING_PANEL);
        petContainer.setBackgroundResource(R.drawable.panel_redondeado); // Asignar el drawable como fondo

        // Crear TextView para el nombre
        TextView nameTextView = new TextView(getContext());
        nameTextView.setText("Nombre: " + pet.getName());
        nameTextView.setTextSize(24);
        nameTextView.setPadding(PADDING_LEFT, PADDING_TOP, PADDING_RIGHT, PADDING_BOTTOM);
        nameTextView.setTextColor(Color.BLACK);
        petContainer.addView(nameTextView);

        TextView rfidView = new TextView(getContext());
        rfidView.setText(pet.getRfid_key());
        rfidView.setTextSize(16);
        rfidView.setPadding(PADDING_LEFT, PADDING_TOP, PADDING_RIGHT, 16);
        petContainer.addView(rfidView);

        // Crear TextView para feed_times
        TextView feedTimesTextView = new TextView(getContext());
        feedTimesTextView.setText("Cantidad de Comidas: " + pet.getFeed_times());
        feedTimesTextView.setTextSize(16);
        feedTimesTextView.setPadding(PADDING_LEFT, PADDING_TOP, PADDING_RIGHT, PADDING_BOTTOM);
        petContainer.addView(feedTimesTextView);

        // Crear TextView para food_amount
        TextView foodAmountTextView = new TextView(getContext());
        foodAmountTextView.setText("Cantidad de comida: " + pet.getFood_amount());
        foodAmountTextView.setTextSize(16);
        foodAmountTextView.setPadding(PADDING_LEFT, PADDING_TOP, PADDING_RIGHT, PADDING_BOTTOM);
        petContainer.addView(foodAmountTextView);

        // Crear TextView para eat_average
        TextView eatAverageTextView = new TextView(getContext());
        eatAverageTextView.setText("Promedio ingerido: " + String.format("%.2f",pet.getEat_average()));
        eatAverageTextView.setTextSize(16);
        eatAverageTextView.setPadding(PADDING_LEFT, PADDING_TOP, PADDING_RIGHT, 16);
        petContainer.addView(eatAverageTextView);



        // Agregar el contenedor del Pet al contenedor principal
        container.addView(petContainer);

        // Añadir un margen inferior al contenedor del Pet
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) petContainer.getLayoutParams();
        params.setMargins(0, 0, 0, 16);
        petContainer.setLayoutParams(params);
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void editPetDialog(Pet pet) {
        LayoutInflater inflater = getLayoutInflater();
        View popupView = inflater.inflate(R.layout.popup_pet_layout, null);

        // Crear el AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(getContext(), R.style.CustomDialogTheme));
        builder.setView(popupView)
                .setTitle("Editar Mascota")
                .setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Acción al hacer clic en "Aceptar"
                        EditText input = popupView.findViewById(R.id.popup_input);
                        String userInput = input.getText().toString();

                        pet.setName(userInput);
                        mainActivity.petRecorder.updatePet(pet);
                        mainActivity.petRecorder.savePetsToFile(getContext());
                        LinearLayout containerLayout = binding.getRoot().findViewById(R.id.contenedor_linear);
                        LinearLayout petLayout = findPetCard(containerLayout,pet);
                        if(petLayout != null) {
                            updatePetCard(petLayout,pet);
                        }

                    }
                })
                .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Acción al hacer clic en "Cancelar"
                        dialog.dismiss();
                    }
                });

        // Mostrar el AlertDialog
        dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                EditText input = popupView.findViewById(R.id.popup_input);
                input.requestFocus();
                input.setText(pet.getName());
                InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);

                // cambio color texto botones
                int textColor = android.graphics.Color.argb(255, 0, 0, 0);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(textColor);
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(textColor);
            }
        });
        dialog.show();

    }

}