package douglasghiraldello.cursoandroid.uber.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

import douglasghiraldello.cursoandroid.uber.R;
import douglasghiraldello.cursoandroid.uber.config.ConfiguracaoFirebase;
import douglasghiraldello.cursoandroid.uber.helper.UsuarioFirebase;
import douglasghiraldello.cursoandroid.uber.model.Usuario;

public class LoginActivity extends AppCompatActivity {

    private Button botaoEntrarLogin;
    private EditText editSenha, editEmail;
    private FirebaseAuth autenticacao;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        inicializarComponentes();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


    }

    public void validarLoginUsuario(View view) {

        //Recuperar os textos dos campos
        String textoEmail = editEmail.getText().toString();
        String textoSenha = editSenha.getText().toString();

        if (!textoEmail.isEmpty()) {
            if (!textoSenha.isEmpty()) {

                Usuario usuario = new Usuario();
                usuario.setEmail(textoEmail);
                usuario.setSenha(textoSenha);

                logarUsuario(usuario);

            } else {
                Toast.makeText(this,
                        "Preencha a senha!",
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this,
                    "Preencha o e-mail!",
                    Toast.LENGTH_SHORT).show();
        }

    }

    public void logarUsuario(Usuario usuario) {

        autenticacao = ConfiguracaoFirebase.getFirebaseAutenticacao();
        autenticacao.signInWithEmailAndPassword(
                usuario.getEmail(),
                usuario.getSenha()
        ).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {

                    //Verifica e redireciona o tipo de usuario para sua respectiva tela
                    UsuarioFirebase.redirecionaUsuarioLogado(LoginActivity.this);

                } else {
                    String excecao = "";
                    try {
                        throw task.getException();
                    } catch (FirebaseAuthWeakPasswordException e) {
                        excecao = "Digite uma senha mais forte!";
                    } catch (FirebaseAuthInvalidCredentialsException e) {
                        excecao = "Por favor, digite um e-mail válido";
                    } catch (FirebaseAuthUserCollisionException e) {
                        excecao = "Este conta já foi cadastrada";
                    } catch (Exception e) {
                        excecao = "Erro ao efetuar login " + e.getMessage();
                        e.printStackTrace();
                    }
                    Toast.makeText(LoginActivity.this,
                            excecao,
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    public void inicializarComponentes() {

        botaoEntrarLogin = findViewById(R.id.buttonEntrarLogin);
        editEmail = findViewById(R.id.editEmailLogin);
        editSenha = findViewById(R.id.editSenhaLogin);

    }

    //Meotodo utilizado para voltar a acativity anterior
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return false;
    }


}
