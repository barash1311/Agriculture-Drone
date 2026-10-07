package com.agridrone.safety.ui.main.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.agridrone.safety.R;
import com.agridrone.safety.data.model.BatteryCell;
import com.agridrone.safety.databinding.ItemBatteryCellBinding;
import com.agridrone.safety.util.NumberFormatter;

import java.util.ArrayList;
import java.util.List;

public final class CellVoltageAdapter extends RecyclerView.Adapter<CellVoltageAdapter.CellViewHolder> {

    private final List<BatteryCell> cells = new ArrayList<>();

    public void updateCells(List<BatteryCell> newCells) {
        cells.clear();
        if (newCells != null) {
            cells.addAll(newCells);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CellViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBatteryCellBinding binding = ItemBatteryCellBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new CellViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CellViewHolder holder, int position) {
        holder.bind(cells.get(position));
    }

    @Override
    public int getItemCount() {
        return cells.size();
    }

    static final class CellViewHolder extends RecyclerView.ViewHolder {

        private final ItemBatteryCellBinding binding;

        CellViewHolder(ItemBatteryCellBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(BatteryCell cell) {
            binding.textCellIndex.setText(cell.getDisplayLabel());
            binding.textCellVoltage.setText(NumberFormatter.formatCellVoltage(cell.getVoltageVolts()));

            // Highlight hierarchy: Fault > Lowest > Neutral
            if (cell.isFault()) {
                binding.layoutCellContainer.setBackgroundResource(R.drawable.bg_cell_item_fault);
            } else if (cell.isLowest()) {
                binding.layoutCellContainer.setBackgroundResource(R.drawable.bg_cell_item_lowest);
            } else {
                binding.layoutCellContainer.setBackgroundResource(R.drawable.bg_cell_item);
            }
        }
    }
}
