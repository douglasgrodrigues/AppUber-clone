package douglasghiraldello.cursoandroid.uber.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import douglasghiraldello.cursoandroid.uber.R;
import douglasghiraldello.cursoandroid.uber.adapter.RequisicoesAdapter;
import douglasghiraldello.cursoandroid.uber.config.ConfiguracaoFirebase;
import douglasghiraldello.cursoandroid.uber.helper.RecyclerItemClickListener;
import douglasghiraldello.cursoandroid.uber.helper.UsuarioFirebase;
import douglasghiraldello.cursoandroid.uber.model.Requisicao;
import douglasghiraldello.cursoandroid.uber.model.Usuario;

public class RequisicoesActivity extends AppCompatActivity {

    private RecyclerView recyclerRequisicoes;
    private TextView textResultado;

    private FirebaseAuth autenticacao;
    private DatabaseReference firebaseRef;
    private List<Requisicao> listaRequisicoes = new ArrayList<>();
    private RequisicoesAdapter adapter;
    private Usuario motorista;

    private LocationManager locationManager;
    private LocationListener locationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_requisicoes);

        inicializarComponentes();
        recuperarLocalizacaoUsuario();
    }

    @Override
    protected void onStart() {
        super.onStart();
        verificaStatusRequisicao();
    }

    private void verificaStatusRequisicao() {
        Usuario usuarioLogado = UsuarioFirebase.getDadosUsuarioLogado();
        DatabaseReference firebaseRef = ConfiguracaoFirebase.getFirebaseDatabase();

        DatabaseReference requisicoes = firebaseRef.child("requisicoes");

        Query requisicoesPesquisa = requisicoes.orderByChild("motorista/id")
                .equalTo(usuarioLogado.getId());

        requisicoesPesquisa.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {

                    Requisicao requisicao = ds.getValue(Requisicao.class);

                    //Verifica o status da requisicao para listar as que estao ativas
                    if (requisicao.getStatus().equals(Requisicao.STATUS_A_CAMINHO)
                            || requisicao.getStatus().equals(Requisicao.STATUS_VIAGEM)
                            || requisicao.getStatus().equals(Requisicao.STATUS_FINALIZADA)
                    ) {

                        motorista = requisicao.getMotorista();
                        abrirTelaCorrida(requisicao.getId(), motorista, true);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void recuperarLocalizacaoUsuario() {
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                String latitude = String.valueOf(location.getLatitude());
                String longitude = String.valueOf(location.getLongitude());

                //Atualizar Geofire
                UsuarioFirebase.atualizarDadosLocalizacao(
                        location.getLatitude(),
                        location.getLongitude()
                );

                if (!latitude.isEmpty() && !longitude.isEmpty()) {
                    motorista.setLatitude(latitude);
                    motorista.setLongitude(longitude);

                    adicionaEventoCliqueRecyclerView();
                    locationManager.removeUpdates(locationListener);
                    adapter.notifyDataSetChanged();
                }

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(@NonNull String provider) {
            }

            @Override
            public void onProviderDisabled(@NonNull String provider) {
            }
        };
        //Solicitar atualizações de localização
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    0,
                    0,
                    locationListener
            );
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuSair:
                autenticacao.signOut();
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void abrirTelaCorrida(String idRequisicao, Usuario motorista, boolean requisicaoAtiva) {

        Intent i = new Intent(RequisicoesActivity.this, CorridaActivity.class);
        i.putExtra("idRequisicao", idRequisicao);
        i.putExtra("motorista", motorista);
        i.putExtra("requisicaoAtiva", requisicaoAtiva);
        startActivity(i);
    }

    private void inicializarComponentes() {

        getSupportActionBar().setTitle("Resquisições");

        recyclerRequisicoes = findViewById(R.id.recyclerRequisicoes);
        textResultado = findViewById(R.id.textResultado);

        //Configurações iniciais
        motorista = UsuarioFirebase.getDadosUsuarioLogado();
        autenticacao = ConfiguracaoFirebase.getFirebaseAutenticacao();
        firebaseRef = ConfiguracaoFirebase.getFirebaseDatabase();

        //Configurar o Recycler
        adapter = new RequisicoesAdapter(listaRequisicoes, getApplicationContext(), motorista);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerRequisicoes.setLayoutManager(layoutManager);
        recyclerRequisicoes.setHasFixedSize(true);
        recyclerRequisicoes.setAdapter(adapter);

        recuperarRequisicoes();

    }

    private void adicionaEventoCliqueRecyclerView(){

        //Adiciona o evento de clique nas requisições
        recyclerRequisicoes.addOnItemTouchListener(
                new RecyclerItemClickListener(
                        getApplicationContext(),
                        recyclerRequisicoes,
                        new RecyclerItemClickListener.OnItemClickListener() {
                            @Override
                            public void onItemClick(View view, int position) {
                                Requisicao requisicao = listaRequisicoes.get(position);
                                abrirTelaCorrida(requisicao.getId(), motorista, false);

                            }

                            @Override
                            public void onLongItemClick(View view, int position) {

                            }

                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                            }
                        }
                )
        );

    }

    private void recuperarRequisicoes() {
        DatabaseReference requisicoes = firebaseRef.child("requisicoes");

        Query requisicaoPesquisa = requisicoes.orderByChild("status")
                .equalTo(Requisicao.STATUS_AGUARDANDO);

        requisicaoPesquisa.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.getChildrenCount() > 0) {
                    textResultado.setVisibility(View.GONE);
                    recyclerRequisicoes.setVisibility(View.VISIBLE);
                } else {
                    textResultado.setVisibility(View.VISIBLE);
                    recyclerRequisicoes.setVisibility(View.GONE);
                }

                listaRequisicoes.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Requisicao requisicao = ds.getValue(Requisicao.class);
                    listaRequisicoes.add(requisicao);
                }

                adapter.notifyDataSetChanged();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

}
