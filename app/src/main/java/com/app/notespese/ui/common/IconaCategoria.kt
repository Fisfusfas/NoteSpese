package com.app.notespese.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CarRepair
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.graphics.vector.ImageVector

val ICONE_CATEGORIA: List<Pair<String, ImageVector>> = listOf(
    "label"           to Icons.Default.Label,
    "shopping_cart"   to Icons.Default.ShoppingCart,
    "restaurant"      to Icons.Default.Restaurant,
    "local_cafe"      to Icons.Default.LocalCafe,
    "home"            to Icons.Default.Home,
    "directions_car"  to Icons.Default.DirectionsCar,
    "train"           to Icons.Default.Train,
    "flight"          to Icons.Default.Flight,
    "local_hospital"  to Icons.Default.LocalHospital,
    "fitness_center"  to Icons.Default.FitnessCenter,
    "school"          to Icons.Default.School,
    "computer"        to Icons.Default.Computer,
    "smartphone"      to Icons.Default.Smartphone,
    "wifi"            to Icons.Default.Wifi,
    "bolt"            to Icons.Default.Bolt,
    "account_balance" to Icons.Default.AccountBalance,
    "shopping_bag"    to Icons.Default.ShoppingBag,
    "card_giftcard"   to Icons.Default.CardGiftcard,
    "music_note"      to Icons.Default.MusicNote,
    "sports_soccer"   to Icons.Default.SportsSoccer,
    "pets"            to Icons.Default.Pets,
    "car_repair"      to Icons.Default.CarRepair,
    "bar_chart"       to Icons.Default.BarChart,
    "receipt"         to Icons.Default.Receipt,
)

fun iconaCategoria(icona: String): ImageVector =
    ICONE_CATEGORIA.find { it.first == icona }?.second ?: Icons.Default.Label
