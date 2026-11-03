package au.com.foodtruckcentre.app.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Icecream
import androidx.compose.material.icons.rounded.KebabDining
import androidx.compose.material.icons.rounded.LocalPizza
import androidx.compose.material.icons.rounded.LunchDining
import androidx.compose.material.icons.rounded.OutdoorGrill
import androidx.compose.material.icons.rounded.RamenDining
import androidx.compose.material.icons.rounded.RiceBowl
import androidx.compose.material.icons.rounded.SetMeal
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Tapas
import androidx.compose.ui.graphics.vector.ImageVector
import au.com.foodtruckcentre.app.data.SiteContent.Cuisine

fun cuisineIcon(c: Cuisine): ImageVector = when (c) {
    Cuisine.BURGERS -> Icons.Rounded.LunchDining
    Cuisine.MEXICAN -> Icons.Rounded.Tapas
    Cuisine.ASIAN -> Icons.Rounded.RamenDining
    Cuisine.VIETNAMESE -> Icons.Rounded.RiceBowl
    Cuisine.MEDITERRANEAN -> Icons.Rounded.KebabDining
    Cuisine.BBQ -> Icons.Rounded.OutdoorGrill
    Cuisine.PIZZA -> Icons.Rounded.LocalPizza
    Cuisine.SEAFOOD -> Icons.Rounded.SetMeal
    Cuisine.VEGAN -> Icons.Rounded.Spa
    Cuisine.DESSERTS -> Icons.Rounded.Icecream
}
