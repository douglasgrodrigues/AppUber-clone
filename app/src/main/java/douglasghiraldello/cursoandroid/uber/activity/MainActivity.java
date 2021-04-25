package douglasghiraldello.cursoandroid.uber.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.firebase.auth.FirebaseAuth;

import douglasghiraldello.cursoandroid.uber.R;
import douglasghiraldello.cursoandroid.uber.config.ConfiguracaoFirebase;
import douglasghiraldello.cursoandroid.uber.helper.Permissoes;
import douglasghiraldello.cursoandroid.uber.helper.UsuarioFirebase;

public class MainActivity extends AppCompatActivity {

    private Button botaoEntrar;
    private FirebaseAuth autenticacao;
    private String[] permissoes = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().hide(); //Esconder a actionbar

        //validar as permissoes
        Permissoes.validarPermissoes(permissoes, this, 1);

        //inicializa os componentes
        inicializarComponentes();

        //autenticacao = ConfiguracaoFirebase.getFirebaseAutenticacao();
        //autenticacao.signOut();


    }

    public void inicializarComponentes() {
        botaoEntrar = findViewById(R.id.buttonEntrar);
    }

    public void abrirLogin(View view) {
        //OPÇÕES PARA ABRIR UMA NOVA ACTIVITY
        /*
        * Intent i = new Intent(MainActivity.this, LoginActivity.class);
          startActivity(i);
        * */
        startActivity(new Intent(this, LoginActivity.class));

    }

    public void abrirCadastro(View view) {
        startActivity(new Intent(this, CadastroActivity.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        UsuarioFirebase.redirecionaUsuarioLogado(MainActivity.this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int permissaoResultado : grantResults){
            if (permissaoResultado == PackageManager.PERMISSION_DENIED){
                alertaValidacaoPermissao();
            }
        }
    }

    private void alertaValidacaoPermissao(){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permissões Negadas");
        builder.setMessage("Para utilizar o app é necessário aceitar as permissões");
        builder.setCancelable(false);
        builder.setPositiveButton("Confirmar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }



}