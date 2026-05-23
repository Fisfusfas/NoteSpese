package com.app.notespese.data.repository

import com.app.notespese.data.model.Utente
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : AuthRepository {

    /**
     * Fonte di verità per lo stato auth: emette ogni volta che Firebase Auth cambia utente.
     * callbackFlow + addAuthStateListener è il pattern corretto per convertire callback → Flow.
     * distinctUntilChanged evita re-emission se lo stato non è cambiato (es. token refresh).
     */
    override val utenteCorrente: Flow<Utente?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.toUtente())
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    /**
     * Scambia il Google ID token con una credenziale Firebase e completa il sign-in.
     * Dopo l'autenticazione sincronizza il profilo utente su Firestore (merge, non overwrite).
     */
    override suspend fun signIn(googleIdToken: String): Result<Utente> = runCatching {
        val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        val result = firebaseAuth.signInWithCredential(firebaseCredential).await()
        val fbUser = result.user ?: error("Utente Firebase nullo dopo il sign-in")
        val utente = fbUser.toUtente()
        syncUtenteFirestore(utente)
        utente
    }

    /**
     * Sign-in anonimo per la modalità demo.
     * Richiede che "Accesso anonimo" sia abilitato su Firebase Console > Authentication > Sign-in method.
     */
    override suspend fun signInAnonymously(): Result<Utente> = runCatching {
        val result = firebaseAuth.signInAnonymously().await()
        val fbUser = result.user ?: error("Utente anonimo Firebase nullo")
        Utente(
            id      = fbUser.uid,
            nome    = "Utente Demo",
            email   = "",
            fotoUrl = null
        )
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
    }

    /**
     * Crea o aggiorna il documento users/{uid} su Firestore.
     * SetOptions.merge() garantisce che campi non presenti nella mappa non vengano cancellati
     * (es. fcmToken scritto da FCM Service in un secondo momento).
     */
    private suspend fun syncUtenteFirestore(utente: Utente) {
        firestore.collection("users")
            .document(utente.id)
            .set(
                mapOf(
                    "nome"    to utente.nome,
                    "email"   to utente.email,
                    "fotoUrl" to utente.fotoUrl,
                ),
                SetOptions.merge()
            )
            .await()
    }

    private fun FirebaseUser.toUtente() = Utente(
        id      = uid,
        nome    = displayName ?: "Utente",
        email   = email ?: "",
        fotoUrl = photoUrl?.toString()
    )
}
