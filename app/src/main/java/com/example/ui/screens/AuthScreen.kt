package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.attemptAutoSignIn
import com.example.data.onGoogleSignInClicked
import com.example.ui.theme.BrokerBlue
import com.example.ui.theme.BrokerGreen
import com.example.ui.theme.BrokerIndigo
import com.example.ui.theme.BrokerTeal

@Composable
fun AuthScreen(
  onAuthSuccess: (preferredName: String?, preferredPhone: String?) -> Unit,
) {
  val context = LocalContext.current
  val focusManager = LocalFocusManager.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  var isCreateAccountMode by remember { mutableStateOf(false) }
  var fullName by remember { mutableStateOf("") }
  var phoneNumber by remember { mutableStateOf("") }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  BackHandler(enabled = isCreateAccountMode) {
    isCreateAccountMode = false
    errorMessage = null
  }

  LaunchedEffect(Unit) {
    attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = { onAuthSuccess(null, null) },
      onUnauthenticated = {},
      scope = scope,
    )
  }

  fun submitAuth() {
    errorMessage = null
    focusManager.clearFocus()
    if (isCreateAccountMode) {
      val cleanName = fullName.trim()
      val cleanPhone = phoneNumber.trim()
      if (cleanName.length < 2) {
        errorMessage = "Please enter your full name."
        return
      }
      if (cleanPhone.filter(Char::isDigit).length < 10) {
        errorMessage = "Please enter a valid 10-digit mobile number."
        return
      }
    }
    isLoading = true
    onGoogleSignInClicked(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = {
        isLoading = false
        if (isCreateAccountMode) {
          onAuthSuccess(fullName.trim(), phoneNumber.trim())
        } else {
          onAuthSuccess(null, null)
        }
      },
      onAuthError = { msg ->
        isLoading = false
        errorMessage = msg
      },
      scope = scope,
      onAuthCancelled = { isLoading = false },
    )
  }

  Box(
    contentAlignment = Alignment.Center,
    modifier =
      Modifier.fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .safeDrawingPadding()
        .imePadding(),
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier =
        Modifier.widthIn(max = 520.dp)
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp, vertical = 24.dp),
    ) {
      // Branded Hero Emblem
      Surface(
        shape = CircleShape,
        shadowElevation = 6.dp,
        modifier = Modifier.size(72.dp),
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier.fillMaxSize()
              .background(Brush.linearGradient(listOf(BrokerIndigo, BrokerBlue))),
        ) {
          Icon(
            imageVector = Icons.Filled.HomeWork,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(36.dp),
          )
        }
      }

      Spacer(Modifier.height(14.dp))

      Text(
        text = stringResource(R.string.app_name),
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        color = BrokerIndigo,
        textAlign = TextAlign.Center,
      )

      Spacer(Modifier.height(4.dp))

      Text(
        text =
          if (isCreateAccountMode) "Create your verified Property Broker account"
          else "Buy, Sell, Rent & Heavy Deposit — Verified Listings",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )

      Spacer(Modifier.height(22.dp))

      // Login / Sign Up Mode Switcher
      SingleChoiceSegmentedButtonRow(
        modifier = Modifier.fillMaxWidth().testTag("auth_mode_switcher"),
      ) {
        SegmentedButton(
          selected = !isCreateAccountMode,
          onClick = {
            errorMessage = null
            isCreateAccountMode = false
          },
          shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
          colors =
            SegmentedButtonDefaults.colors(
              activeContainerColor = BrokerIndigo,
              activeContentColor = Color.White,
            ),
          modifier = Modifier.testTag("auth_tab_login"),
        ) {
          Text("Login", fontWeight = FontWeight.SemiBold)
        }
        SegmentedButton(
          selected = isCreateAccountMode,
          onClick = {
            errorMessage = null
            isCreateAccountMode = true
          },
          shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
          colors =
            SegmentedButtonDefaults.colors(
              activeContainerColor = BrokerIndigo,
              activeContentColor = Color.White,
            ),
          modifier = Modifier.testTag("auth_tab_signup"),
        ) {
          Text("Sign Up", fontWeight = FontWeight.SemiBold)
        }
      }

      Spacer(Modifier.height(16.dp))

      // Main Auth Card
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          verticalArrangement = Arrangement.spacedBy(14.dp),
          modifier = Modifier.fillMaxWidth().padding(20.dp),
        ) {
          Text(
            text = if (isCreateAccountMode) "New Member Sign Up" else "Welcome Back",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = BrokerIndigo,
          )

          AnimatedVisibility(
            visible = isCreateAccountMode,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
              OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it.take(80) },
                label = { Text("Full Name *") },
                placeholder = { Text("Enter your full name") },
                leadingIcon = {
                  Icon(Icons.Filled.Person, contentDescription = null, tint = BrokerIndigo)
                },
                keyboardOptions =
                  KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                  ),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("signup_name_input"),
              )

              OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it.filter { ch -> ch.isDigit() || ch == '+' }.take(15) },
                label = { Text("Mobile Number *") },
                placeholder = { Text("10-digit mobile number") },
                leadingIcon = {
                  Icon(Icons.Filled.Phone, contentDescription = null, tint = BrokerIndigo)
                },
                keyboardOptions =
                  KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Done,
                  ),
                keyboardActions = KeyboardActions(onDone = { submitAuth() }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("signup_phone_input"),
              )
            }
          }

          Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.VerifiedUser,
                contentDescription = null,
                tint = BrokerGreen,
                modifier = Modifier.size(18.dp),
              )
              Spacer(Modifier.width(8.dp))
              Text(
                text =
                  if (isCreateAccountMode)
                    "Instant Google verification secures your profile, listings, and saved alerts across devices."
                  else
                    "One-tap verified Google Sign-In keeps your listings, chats, and contacts synced securely.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
              )
            }
          }

          if (errorMessage != null) {
            Surface(
              color = MaterialTheme.colorScheme.errorContainer,
              shape = MaterialTheme.shapes.small,
              modifier = Modifier.fillMaxWidth().testTag("auth_error_banner"),
            ) {
              Text(
                text = errorMessage ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(12.dp),
              )
            }
          }

          Button(
            onClick = { submitAuth() },
            enabled = !isLoading,
            shape = RoundedCornerShape(12.dp),
            colors =
              ButtonDefaults.buttonColors(
                containerColor = BrokerBlue,
                contentColor = Color.White,
              ),
            modifier =
              Modifier.fillMaxWidth()
                .height(54.dp)
                .testTag(if (isCreateAccountMode) "signup_submit_button" else "google_sign_in_button"),
          ) {
            if (isLoading) {
              CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.dp,
                modifier = Modifier.size(22.dp),
              )
              Spacer(Modifier.width(10.dp))
              Text("Signing in...", fontWeight = FontWeight.Bold)
            } else {
              Text(
                text =
                  if (isCreateAccountMode) "Sign Up with Google"
                  else stringResource(R.string.sign_in_with_google),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
            }
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text =
                if (isCreateAccountMode) "Already have an account?"
                else "New to Property Broker?",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(
              onClick = {
                errorMessage = null
                isCreateAccountMode = !isCreateAccountMode
              },
              modifier =
                Modifier.testTag(
                  if (isCreateAccountMode) "go_to_login_button" else "create_account_button"
                ),
            ) {
              Text(
                text = if (isCreateAccountMode) "Login" else "Create Account",
                fontWeight = FontWeight.Bold,
                color = BrokerIndigo,
              )
            }
          }
        }
      }

      Spacer(Modifier.height(18.dp))

      // Compact Platform Value Highlights
      Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        AuthFeaturePill(
          icon = Icons.Filled.VerifiedUser,
          label = "Verified Contacts",
          tint = BrokerGreen,
          modifier = Modifier.weight(1f),
        )
        AuthFeaturePill(
          icon = Icons.Filled.Forum,
          label = "Direct Chat",
          tint = BrokerBlue,
          modifier = Modifier.weight(1f),
        )
        AuthFeaturePill(
          icon = Icons.Filled.NotificationsActive,
          label = "Instant Alerts",
          tint = BrokerTeal,
          modifier = Modifier.weight(1f),
        )
      }

      Spacer(Modifier.height(16.dp))

      Text(
        text = "By continuing, you agree to Property Broker's Terms & Conditions and Privacy Standards.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )

      Spacer(Modifier.height(12.dp))

      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().testTag("auth_rera_disclaimer_card"),
      ) {
        Column(
          verticalArrangement = Arrangement.spacedBy(4.dp),
          modifier = Modifier.fillMaxWidth().padding(12.dp),
        ) {
          Text(
            text = stringResource(R.string.rera_disclaimer_title),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = stringResource(R.string.rera_disclaimer_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 16.sp,
          )
        }
      }
    }
  }
}

@Composable
private fun AuthFeaturePill(
  icon: ImageVector,
  label: String,
  tint: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(12.dp),
    shadowElevation = 1.dp,
    modifier = modifier,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(4.dp),
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(18.dp),
      )
      Text(
        text = label,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        maxLines = 1,
      )
    }
  }
}
