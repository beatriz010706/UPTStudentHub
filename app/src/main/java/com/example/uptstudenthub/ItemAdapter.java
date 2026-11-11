package com.example.uptstudenthub;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/** Adapter personalizado da ListView de dias, com o padrão ViewHolder (RF2). */
public final class ItemAdapter extends BaseAdapter {

    private final Context context;
    private final LayoutInflater inflater;
    private List<DailyForecast> items = Collections.emptyList();
    private Set<String> favoriteDates = Collections.emptySet();
    private boolean celsius = true;

    /** @param context contexto usado para inflar linhas e obter textos */
    public ItemAdapter(Context context) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
    }

    /**
     * Substitui os dados mostrados.
     *
     * @param items         dias a mostrar
     * @param favoriteDates datas (yyyy-MM-dd) marcadas como favoritas no local atual
     * @param celsius       true se a unidade é Celsius
     */
    public void setData(List<DailyForecast> items, Set<String> favoriteDates, boolean celsius) {
        this.items = items;
        this.favoriteDates = favoriteDates;
        this.celsius = celsius;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() { return items.size(); }

    @Override
    public DailyForecast getItem(int position) { return items.get(position); }

    @Override
    public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder h;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_day, parent, false);
            h = new ViewHolder(convertView);
            convertView.setTag(h); // guarda as Views: findViewById só corre uma vez por linha
        } else {
            h = (ViewHolder) convertView.getTag();
        }
        DailyForecast d = items.get(position);
        h.icon.setImageResource(WeatherCodes.iconFor(d.weatherCode));
        h.day.setText(Format.day(d.date));
        h.description.setText(WeatherCodes.describeOrUnknown(d.weatherCode));
        h.temps.setText(context.getString(R.string.ex_row_temps,
                Format.temperature(context, d.tempMax, celsius),
                Format.temperature(context, d.tempMin, celsius)));
        h.rain.setText(Format.rain(context, d.precipProbability));
        h.favorite.setVisibility(favoriteDates.contains(d.date) ? View.VISIBLE : View.GONE);
        return convertView;
    }

    /** Guarda as Views de uma linha. */
    private static final class ViewHolder {
        final ImageView icon;
        final TextView day;
        final TextView description;
        final TextView temps;
        final TextView rain;
        final ImageView favorite;

        ViewHolder(View root) {
            icon = root.findViewById(R.id.ivIcon);
            day = root.findViewById(R.id.tvDay);
            description = root.findViewById(R.id.tvDescription);
            temps = root.findViewById(R.id.tvTemps);
            rain = root.findViewById(R.id.tvRain);
            favorite = root.findViewById(R.id.ivFavorite);
        }
    }
}
