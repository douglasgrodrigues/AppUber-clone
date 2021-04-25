package douglasghiraldello.cursoandroid.uber.activity;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import douglasghiraldello.cursoandroid.uber.R;
import douglasghiraldello.cursoandroid.uber.config.ConfiguracaoFirebase;
import douglasghiraldello.cursoandroid.uber.helper.Local;
import douglasghiraldello.cursoandroid.uber.helper.UsuarioFirebase;
import douglasghiraldello.cursoandroid.uber.model.Destino;
import douglasghiraldello.cursoandroid.uber.model.Requisicao;
import douglasghiraldello.cursoandroid.uber.model.Usuario;

public class PassageiroActivity extends AppCompatActivity
        implements OnMapReadyCallback {

    /*
     * lat/log destino: -30.02286674906069, -51.20931815489042
     * lat/log passageiro: -30.017509, -51.208256
     * lat/log motorista:
     *   inicial: -30.015732307814, -51.208084338751895
     *   intermediario: -30.01669846010073, -51.20776247367591
     *   final: -30.01729765558816, -51.208583229619684
     * */


    private GoogleMap mMap;
    private FirebaseAuth autenticacao;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private LatLng localPassageiro;
    private boolean cancelarUber = false;
    private DatabaseReference firebaseRef;
    private Requisicao requisicao;
    private Usuario passageiro;
    private String statusRequisicao;
    private Destino destino;
    private Marker marcadorMotorista;
    private Marker marcadorPassageiro;
    private Marker marcadorDestino;
    private Usuario motorista;
    private LatLng localMotorista;

    private EditText editDestino;
    private LinearLayout linearLayoutDestino;
    private Button buttonChamarUber;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passageiro);

        inicializarComponentes();

        //listener para status da requisicao
        verificaStatusRequisicao();

    }

    private void verificaStatusRequisicao() {

        Usuario usuarioLogado = UsuarioFirebase.getDadosUsuarioLogado();
        DatabaseReference requisicoes = firebaseRef.child("requisicoes");
        Query requisicaoPesquisa = requisicoes.orderByChild("passageiro/id")
                .equalTo(usuarioLogado.getId());
        requisicaoPesquisa.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Requisicao> lista = new ArrayList<>();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    lista.add(ds.getValue(Requisicao.class));
                }

                Collections.reverse(lista);

                if (lista != null && lista.size() > 0) {
                    requisicao = lista.get(0);

                    if (requisicao != null) {
                        if (!requisicao.getStatus().equals(Requisicao.STATUS_ENCERRADA)) {

                            passageiro = requisicao.getPassageiro();
                            localPassageiro = new LatLng(
                                    Double.parseDouble(passageiro.getLatitude()),
                                    Double.parseDouble(passageiro.getLongitude())
                            );
                            statusRequisicao = requisicao.getStatus();
                            destino = requisicao.getDestino();
                            if (requisicao.getMotorista() != null) {
                                motorista = requisicao.getMotorista();
                                localMotorista = new LatLng(
                                        Double.parseDouble(motorista.getLatitude()),
                                        Double.parseDouble(motorista.getLongitude())
                                );
                            }
                            alteraInterfaceStatusRequisicao(statusRequisicao);
                        }
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private void alteraInterfaceStatusRequisicao(String status) {

        if (status != null && !status.isEmpty()) {
            cancelarUber = false; //Bloqueia o cancelamento da corrida

            switch (status) {
                case Requisicao.STATUS_AGUARDANDO:
                    requisicaoAguardando();
                    break;

                case Requisicao.STATUS_A_CAMINHO:
                    requisicaoACaminho();
                    break;

                case Requisicao.STATUS_VIAGEM:
                    requisicaoViagem();
                    break;

                case Requisicao.STATUS_FINALIZADA:
                    requisicaoFinalizada();
                    break;

                case Requisicao.STATUS_CANCELADA:
                    requisicaoCancelada();
                    break;
            }
        } else {
            //Adicionar o marcador passageiro após finalizar
            adicionarMarcadorPassageiro(localPassageiro, "Meu Local");
            centralizarMarcador(localPassageiro);
        }

    }

    private void requisicaoCancelada(){

        linearLayoutDestino.setVisibility(View.VISIBLE);
        buttonChamarUber.setText("Chamar Uber");
        cancelarUber = false;

    }

    private void requisicaoAguardando() {

        linearLayoutDestino.setVisibility(View.GONE);
        buttonChamarUber.setText("Calcelar corrida");
        cancelarUber = true;

        //Adicionar marcador passageiro
        adicionarMarcadorPassageiro(localPassageiro, passageiro.getNome());
        centralizarMarcador(localPassageiro);
    }

    private void requisicaoACaminho() {

        linearLayoutDestino.setVisibility(View.GONE);
        buttonChamarUber.setText("Motorista a caminho");
        buttonChamarUber.setEnabled(false);

        //Adicionar marcador passageiro
        adicionarMarcadorPassageiro(localPassageiro, passageiro.getNome());

        //Adicionar marcador motorista
        adicionarMarcadorMotorista(localMotorista, motorista.getNome());

        //centralizar marcadores
        centralizarDoisMarcadores(marcadorMotorista, marcadorPassageiro);

    }

    private void requisicaoViagem() {

        linearLayoutDestino.setVisibility(View.GONE);
        buttonChamarUber.setText("A caminho do destino");
        buttonChamarUber.setEnabled(false);

        //Adicionar marcador motorista
        adicionarMarcadorMotorista(localMotorista, motorista.getNome());

        //Adicionar marcador destino
        LatLng localDestino = new LatLng(
                Double.parseDouble(destino.getLatitude()),
                Double.parseDouble(destino.getLongitude())
        );
        adicionarMarcadorDestino(localDestino, "Destino");

        //centralizar marcadores
        centralizarDoisMarcadores(marcadorMotorista, marcadorDestino);

    }

    private void requisicaoFinalizada() {

        linearLayoutDestino.setVisibility(View.GONE);
        buttonChamarUber.setEnabled(false);

        //Adicionar marcador destino
        LatLng localDestino = new LatLng(
                Double.parseDouble(destino.getLatitude()),
                Double.parseDouble(destino.getLongitude())
        );
        adicionarMarcadorDestino(localDestino, "Destino");
        centralizarMarcador(localDestino);

        //calcular distancia
        float distancia = Local.calcularDistancia(localPassageiro, localDestino);
        float valor = distancia * 4;
        //Formata para decimal
        DecimalFormat decimal = new DecimalFormat("0.00");
        String resultado = decimal.format(valor);

        buttonChamarUber.setText("Corrida finalizada: Total R$ " + resultado);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Total da viagem")
                .setMessage("Sua viagem ficou: R$ " + resultado)
                .setCancelable(false)
                .setNegativeButton("Encerrar viagem", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        requisicao.setStatus(Requisicao.STATUS_ENCERRADA);
                        requisicao.atualizarStatus();

                        finish();
                        startActivity(new Intent(getIntent())); //Finalizar a activity e retornar para ela mesma
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void adicionarMarcadorPassageiro(LatLng localizacao, String titulo) {

        if (marcadorPassageiro != null)
            marcadorPassageiro.remove();

        marcadorPassageiro = mMap.addMarker(
                new MarkerOptions()
                        .position(localizacao)
                        .title(titulo)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.usuario))
        );
    }

    private void centralizarMarcador(LatLng local) {
        mMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(local, 20)
        );
    }

    private void adicionarMarcadorMotorista(LatLng localizacao, String titulo) {

        if (marcadorMotorista != null)
            marcadorMotorista.remove();

        marcadorMotorista = mMap.addMarker(
                new MarkerOptions()
                        .position(localizacao)
                        .title(titulo)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.car))
        );
    }

    private void adicionarMarcadorDestino(LatLng localizacao, String titulo) {

        if (marcadorPassageiro != null)
            marcadorPassageiro.remove();

        if (marcadorDestino != null)
            marcadorDestino.remove();

        marcadorDestino = mMap.addMarker(
                new MarkerOptions()
                        .position(localizacao)
                        .title(titulo)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.destino))
        );
    }

    private void centralizarDoisMarcadores(Marker marcador1, Marker marcador2) {

        //Define um limite entre os dois pontos []
        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        builder.include(marcador1.getPosition());
        builder.include(marcador2.getPosition());

        LatLngBounds bounds = builder.build();

        // Recupera dados do dispositivo do usuario
        int largura = getResources().getDisplayMetrics().widthPixels;
        int altura = getResources().getDisplayMetrics().heightPixels;
        int espacoInterno = (int) (largura * 0.20);

        mMap.moveCamera(
                CameraUpdateFactory.newLatLngBounds(bounds, largura, altura, 200)
        );
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //recuperar localização do usuario
        recuperarLocalizacaoUsuario();
    }

    public void chamarUber(View view) {

        //False -> Uber não pode ser cancelado
        //True -> Corride pode ser cancelada

        if ( cancelarUber ) { //Pode ser cancelado

            //Cancelar a requisicao
            requisicao.setStatus(Requisicao.STATUS_CANCELADA);
            requisicao.atualizarStatus();

        } else { //Não pode mais ser cancelado
            String enderecoDestino = editDestino.getText().toString();

            if (!enderecoDestino.equals("") || enderecoDestino != null) {
                Address addressDestino = recuperarEndereco(enderecoDestino);
                if (addressDestino != null) {

                    //Recuperando todos os dados digitados
                    final Destino destino = new Destino();
                    destino.setCidade(addressDestino.getSubAdminArea());
                    destino.setCep(addressDestino.getPostalCode());
                    destino.setBairro(addressDestino.getSubLocality());
                    destino.setRua(addressDestino.getThoroughfare());
                    destino.setNumero(addressDestino.getFeatureName());
                    destino.setLatitude(String.valueOf(addressDestino.getLatitude()));
                    destino.setLongitude(String.valueOf(addressDestino.getLongitude()));

                    //concatenando todos os dados em uma string
                    StringBuilder mensagem = new StringBuilder();
                    mensagem.append("Cidade: " + destino.getCidade());
                    mensagem.append("\nRua: " + destino.getRua());
                    mensagem.append("\nBairro: " + destino.getBairro());
                    mensagem.append("\nNumero: " + destino.getNumero());
                    mensagem.append("\nCep: " + destino.getCep());

                    AlertDialog.Builder builder = new AlertDialog.Builder(this)
                            .setTitle("Confirme seu endereço")
                            .setMessage(mensagem)
                            .setPositiveButton("Confirmar", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                    //salvar requisção
                                    salvarRequisicao(destino);

                                }
                            }).setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            } else {
                Toast.makeText(this,
                        "Informe o endereço de destino!",
                        Toast.LENGTH_SHORT).show();
            }
        }

    }

    private void salvarRequisicao(Destino destino) {

        Requisicao requisicao = new Requisicao();
        requisicao.setDestino(destino);

        Usuario usuarioPassageiro = UsuarioFirebase.getDadosUsuarioLogado();
        usuarioPassageiro.setLatitude(String.valueOf(localPassageiro.latitude));
        usuarioPassageiro.setLongitude(String.valueOf(localPassageiro.longitude));

        requisicao.setPassageiro(usuarioPassageiro);
        requisicao.setStatus(Requisicao.STATUS_AGUARDANDO);
        requisicao.salvar();

        //Alterar interface depois de chamar o uber
        linearLayoutDestino.setVisibility(View.GONE);
        buttonChamarUber.setText("Calcelar corrida");


    }

    private Address recuperarEndereco(String endereco) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> listaEnderecos = geocoder.getFromLocationName(endereco, 1);
            if (listaEnderecos != null && listaEnderecos.size() > 0) {
                Address address = listaEnderecos.get(0);

                return address;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void recuperarLocalizacaoUsuario() {
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {

                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                localPassageiro = new LatLng(latitude, longitude);

                //Atualizar Geofire
                UsuarioFirebase.atualizarDadosLocalizacao(latitude, longitude);

                //Alterar a interface de acordo com o status
                alteraInterfaceStatusRequisicao(statusRequisicao);

                if (statusRequisicao != null && !statusRequisicao.isEmpty()) {
                    if (statusRequisicao.equals(Requisicao.STATUS_VIAGEM)
                            || statusRequisicao.equals(Requisicao.STATUS_FINALIZADA)) {
                        locationManager.removeUpdates(locationListener);
                    } else {
                        //Solicitar atualizações de localização
                        if (ActivityCompat.checkSelfPermission(PassageiroActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            locationManager.requestLocationUpdates(
                                    LocationManager.GPS_PROVIDER,
                                    10000,
                                    10,
                                    locationListener
                            );
                        }
                    }
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
                    10000,
                    10,
                    locationListener
            );
        }


    }

    private void inicializarComponentes() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Iniciar uma viagem");
        setSupportActionBar(toolbar);

        //Configurações iniciais
        autenticacao = ConfiguracaoFirebase.getFirebaseAutenticacao();
        firebaseRef = ConfiguracaoFirebase.getFirebaseDatabase();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        editDestino = findViewById(R.id.editDestino);
        linearLayoutDestino = findViewById(R.id.linerarLayoutDestino);
        buttonChamarUber = findViewById(R.id.buttonChamarUber);

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
}
