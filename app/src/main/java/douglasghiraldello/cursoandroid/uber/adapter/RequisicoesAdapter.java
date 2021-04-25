package douglasghiraldello.cursoandroid.uber.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

import douglasghiraldello.cursoandroid.uber.R;
import douglasghiraldello.cursoandroid.uber.helper.Local;
import douglasghiraldello.cursoandroid.uber.model.Requisicao;
import douglasghiraldello.cursoandroid.uber.model.Usuario;

public class RequisicoesAdapter extends RecyclerView.Adapter<RequisicoesAdapter.MyViewHolder> {

    private List<Requisicao> requisicoes;
    private Context context;
    private Usuario motorista;

    public RequisicoesAdapter(List<Requisicao> requisicoes, Context context, Usuario motorista) {
        this.requisicoes = requisicoes;
        this.context = context;
        this.motorista = motorista;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View item = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_requisicoes, parent, false);

        return new MyViewHolder(item);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        Requisicao requisicao = requisicoes.get(position);
        Usuario passageiro = requisicao.getPassageiro();

        holder.nome.setText(passageiro.getNome());

        if (motorista != null){

            LatLng localPassageiro = new LatLng(
                    Double.parseDouble(passageiro.getLatitude()),
                    Double.parseDouble(passageiro.getLongitude())
            );

            LatLng localMotorista = new LatLng(
                    Double.parseDouble(motorista.getLatitude()),
                    Double.parseDouble(motorista.getLongitude())
            );

            float distancia = Local.calcularDistancia(localPassageiro, localMotorista);
            String distanciaFormatada = Local.formatarDistancia(distancia);
            holder.distancia.setText(distanciaFormatada + " - aproximadamente");
        }
    }

    @Override
    public int getItemCount() {
        return requisicoes.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder{
        TextView nome, distancia;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            nome = itemView.findViewById(R.id.textRequisicaoNome);
            distancia = itemView.findViewById(R.id.textRequisicaoDistancia);
        }
    }

}
