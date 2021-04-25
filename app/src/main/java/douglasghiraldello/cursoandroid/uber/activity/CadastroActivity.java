package douglasghiraldello.cursoandroid.uber.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
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

public class CadastroActivity extends AppCompatActivity {

    private Button buttonCadastrar;
    private EditText editNome, editEmail, editSenha;
    private FirebaseAuth autenticacao;
    private Usuario usuario;
    private Switch tipoUsuario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro);

        inicializarComponentes();
        //habilita o botão de voltar paar a activity anterior
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }

    public void inicializarComponentes() {
        buttonCadastrar = findViewById(R.id.buttonCadastrar);
        editNome = findViewById(R.id.editNomeCadastro);
        editEmail = findViewById(R.id.editEmailCadastro);
        editSenha = findViewById(R.id.editSenhaCadastro);
        tipoUsuario = findViewById(R.id.switchTipo);
    }

    public void validarCadastroUsuario(View view) {

        String textoNome = editNome.getText().toString();
        String textoEmail = editEmail.getText().toString();
        String textoSenha = editSenha.getText().toString();

        if (!textoNome.isEmpty()) {
            if (!textoEmail.isEmpty()) {
                if (!textoSenha.isEmpty()) {

                    Usuario usuario = new Usuario();
                    usuario.setNome(textoNome);
                    usuario.setEmail(textoEmail);
                    usuario.setSenha(textoSenha);
                    usuario.setTipo(verificaTipoUsuario());

                    cadastrarUsuario(usuario);


                } else {
                    Toast.makeText(this,
                            "Preencha a senha!",
                            Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this,
                        "Preencha o E-mail!",
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this,
                    "Preencha o nome!",
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void cadastrarUsuario(final Usuario usuario) {

        autenticacao = ConfiguracaoFirebase.getFirebaseAutenticacao();
        autenticacao.createUserWithEmailAndPassword(
                usuario.getEmail(),
                usuario.getSenha()
        ).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {

                    try {

                        String idUsuario = task.getResult().getUser().getUid();
                        usuario.setId(idUsuario);
                        usuario.salvar();

                        //Salvar o nome do usuário no UserProfile do dispositivo
                        UsuarioFirebase.atualizarNomeUsuario(usuario.getNome());


                        //direciona o usuario com base no seu tipo
                        //se for passageiro chama o maps
                        //senão chama as requisições

                        if (verificaTipoUsuario() == "P") {
                            startActivity(new Intent(CadastroActivity.this, PassageiroActivity.class)); //direciona para a activity Maps
                            finish(); //Fecha a activity anterior

                            Toast.makeText(CadastroActivity.this,
                                    "Sucesso ao cadastrar Passageiro",
                                    Toast.LENGTH_SHORT).show();

                        } else {
                            startActivity(new Intent(CadastroActivity.this, RequisicoesActivity.class));
                            finish();

                            Toast.makeText(CadastroActivity.this,
                                    "Sucesso ao cadastrar Motorista",
                                    Toast.LENGTH_SHORT).show();
                        }


                    } catch (Exception e) {
                        e.printStackTrace();

                    }

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
                        excecao = "Erro ao cadastrar usuário: " + e.getMessage();
                        e.printStackTrace();
                    }

                    Toast.makeText(CadastroActivity.this,
                            excecao,
                            Toast.LENGTH_SHORT).show();
                }

            }
        });

    }

    public String verificaTipoUsuario() {
        return tipoUsuario.isChecked() ? "M" : "P";

    }

    //Meotodo utilizado para voltar a acativity anterior
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return false;
    }

}
