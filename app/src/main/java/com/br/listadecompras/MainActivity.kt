package com.br.listadecompras

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    // Declaração da variável WebView para uso posterior
    private lateinit var webview: WebView

    // Método chamado quando a atividade é criada
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Define o layout da atividade

        // Esconde a barra de status (barra de notificações) para o modo fullscreen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Para Android 11 ou superior
            window.decorView.windowInsetsController?.hide(android.view.WindowInsets.Type.statusBars())
        } else {
            // Para versões anteriores ao Android 11
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
        }

        // Inicializa a WebView buscando pelo ID definido no layout
        webview = findViewById(R.id.webview)

        // Configurações do WebView
        webview.settings.apply {
            javaScriptEnabled = true // Ativa o JavaScript
            domStorageEnabled = true // Habilita o armazenamento local
            loadsImagesAutomatically = true // Carrega imagens automaticamente
            setSupportMultipleWindows(false) // Desabilita múltiplas janelas
        }

        // Define o WebViewClient para lidar com a navegação
        webview.webViewClient = object : WebViewClient() {
            // Intercepta URLs e garante que sejam carregadas dentro do WebView
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()

                // Verifica se a URL é do WhatsApp
                if (url.startsWith("whatsapp://")) {
                    // Se for um link do WhatsApp, abre diretamente o WhatsApp
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
                    return true // Impede que o WebView carregue essa URL
                }

                // Caso contrário, carrega a URL dentro do WebView
                view?.loadUrl(url)
                return true
            }

            // Lidando com erros SSL (não recomendado para produção, somente para testes)
            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
            ) {
                handler?.proceed() // Ignora erros SSL
            }

            // Quando uma página começa a carregar
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                // Você pode mostrar um carregamento aqui, se necessário
            }

            // Quando a página termina de carregar
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Você pode esconder o carregamento quando a página terminar
            }
        }

        // Verifica se há um estado salvo; se não houver, carrega a URL inicial
        if (savedInstanceState == null) {
            // Carrega a URL inicial
            webview.loadUrl("https://listasdecompras.created.app/") // URL do seu site
        } else {
            // Se houver um estado salvo, restaura o estado da WebView
            webview.restoreState(savedInstanceState)
        }

        // Passa os dados para a WebView assim que a página for carregada
        val shoppingList = loadShoppingList() // Carrega a lista de compras
        val jsonItems = Gson().toJson(shoppingList) // Converte a lista em JSON
        val javascriptCode = "loadShoppingListFromAndroid($jsonItems);" // Código JS para passar os dados
        webview.evaluateJavascript(javascriptCode, null) // Envia para o WebView
    }

    // Método chamado para salvar o estado da atividade antes de ser destruída
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Salva o estado atual da WebView no Bundle passado
        webview.saveState(outState)
    }

    // Método para lidar com o botão de voltar
    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one ou mais {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        if (webview.canGoBack()) {
            webview.goBack() // Volta para a página anterior no WebView
        } else {
            super.onBackPressed() // Se não houver páginas anteriores, executa a ação padrão
        }
    }

    // Método para salvar a lista de compras no SharedPreferences
    private fun saveShoppingList(items: List<String>) {
        val sharedPreferences: SharedPreferences = getSharedPreferences("ShoppingListPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val json = Gson().toJson(items) // Converte a lista em JSON
        editor.putString("shoppingList", json)
        editor.apply()
    }

    // Método para carregar a lista de compras do SharedPreferences
    private fun loadShoppingList(): List<String> {
        val sharedPreferences: SharedPreferences = getSharedPreferences("ShoppingListPrefs", MODE_PRIVATE)
        val json = sharedPreferences.getString("shoppingList", null)
        return if (json != null) {
            val type = object : com.google.gson.reflect.TypeToken<List<String>>() {}.type
            Gson().fromJson(json, type) // Converte de volta para lista de String
        } else {
            emptyList() // Retorna uma lista vazia se não houver dados salvos
        }
    }

    // Método chamado para restaurar o estado da barra de status ao retornar para o app
    override fun onResume() {
        super.onResume()
        // Esconde novamente a barra de status quando a atividade estiver em primeiro plano
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.decorView.windowInsetsController?.hide(android.view.WindowInsets.Type.statusBars())
        } else {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
        }
    }

    // Garantir que a barra de status continue escondida quando a janela ganhar o foco
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            // Esconde novamente a barra de status
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.decorView.windowInsetsController?.hide(android.view.WindowInsets.Type.statusBars())
            } else {
                window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_IMMERSIVE
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
            }
        }
    }
}
