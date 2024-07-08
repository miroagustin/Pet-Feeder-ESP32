package soa.L6.pet_feeder.ui.home;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import soa.L6.pet_feeder.Model.FeederRecorder;
import soa.L6.pet_feeder.Model.FeederState;
import soa.L6.pet_feeder.Model.Food;
import soa.L6.pet_feeder.Model.Pet;
import soa.L6.pet_feeder.Utils.MQTTManager;
import soa.L6.pet_feeder.Activities.MainActivity;
import soa.L6.pet_feeder.Utils.PetFeederConstants;
import soa.L6.pet_feeder.R;
import soa.L6.pet_feeder.databinding.FragmentHomeBinding;
import android.widget.Button;
import android.widget.Toast;

public class HomeFragment extends Fragment
{

    private static final String NO_DATA_TEXT = "-";
    private static final int NO_DATA_TIME = -1;
    private FragmentHomeBinding binding;
    private AlertDialog dialog;
    private TextView time_label;
    private TextView amount_label;
    private TextView refillLabel;
    private TextView clearNeedLabel;
    private Button modify_schedule_btn;
    private Button feed_now;
    private EditText input_time;
    private EditText input_amount;
    private MainActivity mainActivity;

    private MQTTManager mqttManager;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState)
    {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);
        mainActivity = (MainActivity) requireActivity();
        mqttManager = mainActivity.getMQTTManager();
        mainActivity.homeFragment = this;

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        RecyclerView recyclerView = root.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        input_time = root.findViewById(R.id.input_time);
        input_amount = root.findViewById(R.id.input_quantity);
        time_label = root.findViewById(R.id.next_feeding_time);
        amount_label = root.findViewById(R.id.food_quantity);
        refillLabel = root.findViewById(R.id.txt_aviso_recarga);
        clearNeedLabel = root.findViewById(R.id.txt_aviso_cambio);
        modify_schedule_btn = root.findViewById(R.id.update_button);
        feed_now = root.findViewById(R.id.feed_now_button);
        // Manejar el clic del botón para agregar horarios
        input_time.setOnClickListener(v -> showTimePickerDialog());
        modify_schedule_btn.setOnClickListener(v -> saveNewAlimentacion());
        feed_now.setOnClickListener(v -> feedNowDialog());
        input_time.setFocusable(false); // Esto hace que el EditText no sea enfocable
        input_time.setCursorVisible(false); // Oculta el cursor para que no parezca editable
        input_time.setKeyListener(null); // Desactiva el teclado virtual

        setHomeData(mainActivity.feederState);

        return root;
    }

    private void feedNowDialog()
    {
        LayoutInflater inflater = getLayoutInflater();
        View popupView = inflater.inflate(R.layout.popup_layout, null);

        // Crear el AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(getContext(), R.style.CustomDialogTheme));
        builder.setView(popupView)
                .setTitle("Alimentar Ahora")
                .setPositiveButton("Aceptar", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        // Acción al hacer clic en "Aceptar"
                        EditText input = popupView.findViewById(R.id.popup_input);
                        String userInput = input.getText().toString();

                        if (!userInput.isEmpty())
                        {
                            String message = "hh;" + userInput;
                            mqttManager.publishMessage(PetFeederConstants.PUB_TOPIC_HORA_COMIDA, message);
                        } else
                        {
                            Toast.makeText(getContext(), "Por favor completa el campo de comida", Toast.LENGTH_SHORT).show();
                        }

                    }
                })
                .setNegativeButton("Cancelar", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Acción al hacer clic en "Cancelar"
                        dialog.dismiss();
                    }
                });

        // Mostrar el AlertDialog
        dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener()
        {
            @Override
            public void onShow(DialogInterface dialogInterface)
            {
                EditText input = popupView.findViewById(R.id.popup_input);
                input.requestFocus();
                InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
            }
        });
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setVisibility(View.GONE);


    }

    public void acceptDialog()
    {
        if(dialog != null)
        {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.performClick();
        }

    }

    private void showTimePickerDialog()
    {
        TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(), (view, hourOfDay, minute) ->
        {
            String time = String.format("%02d:%02d", hourOfDay, minute);
            input_time.setText(time);
        }, 0, 0, true);
        timePickerDialog.show();
    }
    private void saveNewAlimentacion()
    {
        String time = input_time.getText().toString();
        String amount = input_amount.getText().toString();
        input_time.setText("");
        input_amount.setText("");

        MainActivity mainActivity = (MainActivity) getActivity();
        if (time.isEmpty() || amount.isEmpty())
        {
            // Mostrar toast indicando que algún campo está vacío
            Toast.makeText(getContext(), "Por favor completa ambos campos", Toast.LENGTH_SHORT).show();
        } else
        {
            String message = time + ";" + amount;
            assert mainActivity != null;

            Food newFood = new Food(time,Double.parseDouble(amount));
            FeederRecorder feederRecorder = mainActivity.feederState.getFeederRecorder();
            if(feederRecorder.exists(newFood))
            {
                Food modify = feederRecorder.getFoodList().stream().filter(x -> x.getHour().compareTo(newFood.getHour()) == 0).findAny().get();
                modify.setFood_amount(newFood.getFood_amount());
                feederRecorder.updateFood(modify);
            }
            else
            {
                feederRecorder.addFoodToList(newFood);
            }

            feederRecorder.saveFoodToFile(mainActivity);

            setHomeData(mainActivity.feederState);
            mqttManager.publishMessage(PetFeederConstants.PUB_TOPIC_ALIMENTACION, message);
        }
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        setHomeData(mainActivity.feederState);
    }

    public void setHomeData(FeederState state)
    {
        Log.d(HomeFragment.class.getName(), "Feeder State Updateado: " + state);

        time_label.setText(state.getNextMealTime());
        amount_label.setText(String.format("%.2f",state.getFoodAmount()));
        // Handle refillNeeded
        if (state.isRefillNeed())
        {
            refillLabel.setBackgroundResource(R.drawable.tag_informe);
        } else
        {
            refillLabel.setBackgroundResource(R.drawable.tag_informe_desactivado); // This removes the background drawable
        }

        // Handle clearNeeded
        if (state.isClearNeed())
        {
            clearNeedLabel.setBackgroundResource(R.drawable.tag_informe);
        } else
        {
            clearNeedLabel.setBackgroundResource(R.drawable.tag_informe_desactivado); // This removes the background drawable
        }
        // Debug logs to check the drawable change
        Log.d(HomeFragment.class.getName(), "Refill Label Drawable: " + (state.isRefillNeed() ? "tag_informe" : "tag_informe_desactivado"));
        Log.d(HomeFragment.class.getName(), "Clear Label Drawable: " + (state.isClearNeed() ? "tag_informe" : "tag_informe_desactivado"));
    }
}