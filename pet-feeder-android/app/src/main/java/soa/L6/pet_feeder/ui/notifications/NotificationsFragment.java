package soa.L6.pet_feeder.ui.notifications;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.Comparator;
import java.util.stream.Collectors;

import soa.L6.pet_feeder.Activities.MainActivity;
import soa.L6.pet_feeder.Model.FeederRecorder;
import soa.L6.pet_feeder.Model.Food;
import soa.L6.pet_feeder.Model.PetRecorder;
import soa.L6.pet_feeder.R;
import soa.L6.pet_feeder.databinding.FragmentNotificationsBinding;

public class NotificationsFragment extends Fragment {

    private Button btn_sincro;
    private Button btn_delete;
    private MainActivity mainActivity;

    private FragmentNotificationsBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        NotificationsViewModel notificationsViewModel =
                new ViewModelProvider(this).get(NotificationsViewModel.class);

        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        btn_sincro = root.findViewById(R.id.btn_sincro);
        btn_sincro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sinchronizeApp();
            }

        });

        btn_delete = root.findViewById(R.id.btn_delete);
        btn_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteAppData();
            }

        });

        mainActivity = (MainActivity) requireActivity();

        createFoodCards();

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public void createFoodCards()
    {
        LinearLayout containerLayout = binding.getRoot().findViewById(R.id.contenedor_linear);
        FeederRecorder feeder = mainActivity.feederState.getFeederRecorder();

        for (Food food : feeder.getFoodList().stream().sorted(Comparator.comparing(Food::getHour)).collect(Collectors.toList())) {
            addPetToContainer(containerLayout, food);
        }
    }

    public void deleteAppData(){
        MainActivity mainActivity = (MainActivity) getActivity();
        LinearLayout containerLayout = binding.getRoot().findViewById(R.id.contenedor_linear);

        PetRecorder petRecorder = mainActivity.petRecorder;
        FeederRecorder feederRecorder = mainActivity.feederState.getFeederRecorder();

        petRecorder.clearPetList();
        petRecorder.savePetsToFile(getContext());

        mainActivity.feederState.clearFoodList();

        Toast.makeText(getContext(), "Datos Eliminados", Toast.LENGTH_SHORT).show();

    }

    public void sinchronizeApp(){
        Toast.makeText(getContext(), "App sincronizada", Toast.LENGTH_SHORT).show();
    }

    private static final int PADDING_LEFT = 20;
    private static final int PADDING_TOP = 10;
    private static final int PADDING_RIGHT = 0;
    private static final int PADDING_BOTTOM = 0;
    private static final int PADDING_PANEL = 16;

    private void addPetToContainer(ViewGroup container, Food food) {
        // Crear un contenedor para el Pet
        LinearLayout foodContainer = new LinearLayout(getContext());
        foodContainer.setOrientation(LinearLayout.VERTICAL);
        foodContainer.setPadding(PADDING_PANEL, PADDING_PANEL, PADDING_PANEL, PADDING_PANEL);
        foodContainer.setBackgroundResource(R.drawable.panel_redondeado); // Asignar el drawable como fondo

        // Crear TextView para el nombre
        TextView nameTextView = new TextView(getContext());
        nameTextView.setText("Horario: " + food.getHour());
        nameTextView.setTextSize(24);
        nameTextView.setPadding(PADDING_LEFT, PADDING_TOP, PADDING_RIGHT, PADDING_BOTTOM);
        nameTextView.setTextColor(Color.BLACK);
        foodContainer.addView(nameTextView);

        // Crear TextView para feed_times
        TextView feedTimesTextView = new TextView(getContext());
        feedTimesTextView.setText("Cantidad de Comida: " + food.getFood_amount());
        feedTimesTextView.setTextSize(16);
        feedTimesTextView.setPadding(PADDING_LEFT, PADDING_TOP, PADDING_RIGHT, PADDING_BOTTOM);
        foodContainer.addView(feedTimesTextView);

        // Agregar el contenedor del Pet al contenedor principal
        container.addView(foodContainer);

        // Añadir un margen inferior al contenedor del Pet
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) foodContainer.getLayoutParams();
        params.setMargins(0, 0, 0, 16);
        foodContainer.setLayoutParams(params);
    }
}