<template>
  <q-page class="bg-crivo">
    <div class="page-shell">
      <app-header />

      <hr class="divider-line q-mb-xl" />

      <div class="entrar-grade">
        <section>
          <div class="eyebrow q-mb-sm">{{ modo === 'entrar' ? 'Acesso' : 'Nova conta' }}</div>
          <h1 class="font-display q-mt-none q-mb-sm hero-title">
            {{ modo === 'entrar' ? 'Bem-vindo de volta.' : 'Crie sua conta.' }}
          </h1>
          <p class="text-body1 q-mb-none" style="max-width: 40ch; color: var(--cor-texto-suave)">
            Com uma conta, suas análises ficam salvas no seu histórico pessoal e você pode
            validar cada veredito da IA e guardar suas próprias anotações e evidências.
          </p>
        </section>

        <section class="console-panel q-pa-lg">
          <q-tabs
            v-model="modo"
            dense
            no-caps
            align="left"
            active-color="destaque"
            indicator-color="destaque"
            class="font-mono q-mb-lg tabs-modo"
            @update:model-value="erro = ''"
          >
            <q-tab name="entrar" label="Entrar" />
            <q-tab name="cadastro" label="Criar conta" />
          </q-tabs>

          <q-form ref="formRef" class="column" style="gap: 6px" @submit="enviar">
            <q-input
              v-if="modo === 'cadastro'"
              v-model="nome"
              label="Nome"
              autocomplete="name"
              filled
              color="primary"
              maxlength="100"
              :disable="carregando"
              :rules="[v => (v && v.trim().length > 0) || 'Informe seu nome']"
            />

            <q-input
              v-model="email"
              label="E-mail"
              type="email"
              autocomplete="email"
              filled
              color="primary"
              maxlength="255"
              :disable="carregando"
              :rules="[
                v => (v && v.trim().length > 0) || 'Informe seu e-mail',
                v => EMAIL_REGEX.test((v || '').trim()) || 'Informe um e-mail válido'
              ]"
            />

            <q-input
              v-model="senha"
              label="Senha"
              :type="mostrarSenha ? 'text' : 'password'"
              :autocomplete="modo === 'entrar' ? 'current-password' : 'new-password'"
              filled
              color="primary"
              maxlength="72"
              :disable="carregando"
              :hint="modo === 'cadastro' ? 'Mínimo de 8 caracteres' : undefined"
              :rules="modo === 'cadastro'
                ? [v => (v && v.length >= 8) || 'A senha deve ter pelo menos 8 caracteres']
                : [v => (v && v.length > 0) || 'Informe sua senha']"
            >
              <template #append>
                <q-btn
                  flat
                  round
                  dense
                  :icon="mostrarSenha ? 'visibility_off' : 'visibility'"
                  :aria-label="mostrarSenha ? 'Ocultar senha' : 'Mostrar senha'"
                  @click="mostrarSenha = !mostrarSenha"
                />
              </template>
            </q-input>

            <q-banner v-if="erro" dense rounded class="bg-negative text-white q-mt-sm">
              <template #avatar><q-icon name="error_outline" /></template>
              {{ erro }}
            </q-banner>

            <q-btn
              type="submit"
              :loading="carregando"
              unelevated
              color="primary"
              class="font-mono text-weight-medium q-mt-md"
              :label="modo === 'entrar' ? 'Entrar' : 'Criar conta'"
            />
          </q-form>
        </section>
      </div>
    </div>
  </q-page>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Notify } from 'quasar'
import AppHeader from '../components/AppHeader.vue'
import { cadastrar, entrar, extrairMensagemErro } from '../services/api.js'
import { iniciarSessao } from '../services/auth.js'

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

const route = useRoute()
const router = useRouter()

const modo = ref(route.query.modo === 'cadastro' ? 'cadastro' : 'entrar')
const nome = ref('')
const email = ref('')
const senha = ref('')
const mostrarSenha = ref(false)
const carregando = ref(false)
const erro = ref('')

async function enviar() {
  carregando.value = true
  erro.value = ''
  try {
    const dados = { email: email.value.trim(), senha: senha.value }
    const { data } = modo.value === 'entrar'
      ? await entrar(dados)
      : await cadastrar({ ...dados, nome: nome.value.trim() })

    iniciarSessao(data)
    Notify.create({
      type: 'positive',
      message: modo.value === 'entrar' ? `Olá, ${data.usuario.nome}!` : 'Conta criada. Bem-vindo!'
    })
    router.replace(destinoSeguro(route.query.redirect))
  } catch (e) {
    erro.value = extrairMensagemErro(e)
  } finally {
    carregando.value = false
  }
}

/** Só aceita caminhos internos, para o "redirect" não levar o usuário a outro site. */
function destinoSeguro(redirect) {
  return typeof redirect === 'string' && redirect.startsWith('/') && !redirect.startsWith('//')
    ? redirect
    : '/'
}
</script>

<style scoped>
.entrar-grade {
  display: grid;
  grid-template-columns: 1fr minmax(0, 420px);
  gap: 48px;
  align-items: start;
}

@media (max-width: 767px) {
  .entrar-grade {
    grid-template-columns: 1fr;
    gap: 28px;
  }
}

.console-panel {
  background: var(--cor-superficie);
  border: 1px solid var(--cor-borda);
  border-radius: 4px;
}

.tabs-modo {
  color: var(--cor-texto-suave);
  border-bottom: 1px solid var(--cor-borda);
}
</style>
