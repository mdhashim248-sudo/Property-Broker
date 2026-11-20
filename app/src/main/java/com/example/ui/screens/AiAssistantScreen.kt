package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.PropertyListing
import com.example.ui.theme.BrokerBlue
import com.example.ui.theme.BrokerDeepPurple
import com.example.ui.theme.BrokerIndigo
import com.example.ui.theme.BrokerPurple
import com.example.ui.viewmodel.AiAssistantMessage
import com.example.ui.viewmodel.PropertyAiViewModel

private data class QuickAiPrompt(
  val label: String,
  val prompt: String,
  val tag: String,
)

private val quickBrokerPrompts =
  listOf(
    QuickAiPrompt(
      label = "Best Match",
      prompt = "Review the available properties in the app and recommend the top options by value and location.",
      tag = "ai_prompt_best_match",
    ),
    QuickAiPrompt(
      label = "Write Property Ad",
      prompt = "Write a clean, high-converting Indian property listing description for a 2BHK Apartment (950 sq.ft) for Sale with parking and metro access.",
      tag = "ai_prompt_write_ad",
    ),
    QuickAiPrompt(
      label = "EMI & Budget Plan",
      prompt = "Calculate estimated monthly EMI, 20% down payment, and stamp duty tips for an ₹85 Lakh property in Maharashtra at 8.5% interest for 20 years.",
      tag = "ai_prompt_emi_plan",
    ),
    QuickAiPrompt(
      label = "Heavy Deposit Guide",
      prompt = "Explain how Heavy Deposit works in India, key legal agreement checks, and how it compares to monthly Rent.",
      tag = "ai_prompt_heavy_deposit",
    ),
  )

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AiAssistantScreen(
  viewModel: PropertyAiViewModel,
  availableProperties: List<PropertyListing>,
  onBack: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  var inputText by remember { mutableStateOf("") }
  val listState = rememberLazyListState()

  LaunchedEffect(uiState.messages.size, uiState.messages.lastOrNull()?.text?.length) {
    if (uiState.messages.isNotEmpty()) {
      listState.animateScrollToItem(uiState.messages.lastIndex)
    }
  }

  val submitPrompt = {
    val text = inputText.trim()
    if (text.isNotEmpty() && !uiState.isGenerating) {
      inputText = ""
      viewModel.askAssistant(text, availableProperties)
    }
  }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Filled.AutoAwesome,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text("AI Property Assistant", fontWeight = FontWeight.Bold)
          }
        },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          if (uiState.messages.isNotEmpty() && !uiState.isGenerating) {
            IconButton(
              onClick = viewModel::clearConversation,
              modifier = Modifier.testTag("ai_clear_chat_button"),
            ) {
              Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear AI conversation")
            }
          }
        },
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = BrokerIndigo,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White,
          ),
      )
    },
  ) { innerPadding ->
    Box(
      contentAlignment = Alignment.TopCenter,
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .imePadding()
          .background(MaterialTheme.colorScheme.background),
    ) {
      Column(modifier = Modifier.widthIn(max = 640.dp).fillMaxSize()) {
        // Conversation Area
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
          if (uiState.messages.isEmpty()) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(14.dp),
              modifier = Modifier.fillMaxSize().padding(20.dp),
            ) {
              Spacer(Modifier.height(12.dp))
              Surface(
                color = BrokerPurple.copy(alpha = 0.12f),
                shape = CircleShape,
                modifier = Modifier.size(64.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = BrokerPurple,
                    modifier = Modifier.size(32.dp),
                  )
                }
              }
              Text(
                text = "Smart Real Estate AI Tool",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = BrokerIndigo,
                textAlign = TextAlign.Center,
              )
              Text(
                text =
                  "Ask about property prices, EMI estimates, listing ads, Heavy Deposit agreements, or recommendations across ${availableProperties.size} active listing(s).",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
              )

              Spacer(Modifier.height(4.dp))

              Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().testTag("ai_quick_prompts_card"),
              ) {
                Column(
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                  modifier = Modifier.fillMaxWidth().padding(14.dp),
                ) {
                  Text(
                    text = "Try Quick AI Tools",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrokerIndigo,
                  )
                  FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth(),
                  ) {
                    quickBrokerPrompts.forEach { item ->
                      AssistChip(
                        onClick = { viewModel.askAssistant(item.prompt, availableProperties) },
                        label = { Text(item.label, fontWeight = FontWeight.SemiBold) },
                        leadingIcon = {
                          Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = BrokerDeepPurple,
                            modifier = Modifier.size(16.dp),
                          )
                        },
                        colors =
                          AssistChipDefaults.assistChipColors(
                            containerColor = BrokerDeepPurple.copy(alpha = 0.08f),
                            labelColor = BrokerIndigo,
                          ),
                        modifier = Modifier.testTag(item.tag),
                      )
                    }
                  }
                }
              }
            }
          } else {
            LazyColumn(
              state = listState,
              contentPadding = PaddingValues(14.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.fillMaxSize().testTag("ai_messages_list"),
            ) {
              items(uiState.messages, key = { it.id }) { msg ->
                AiMessageBubble(message = msg)
              }
            }
          }
        }

        // Transparent Error Banner (with Retry)
        uiState.errorMessage?.let { errorMsg ->
          Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(10.dp),
            modifier =
              Modifier.fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .testTag("ai_error_banner"),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
              ) {
                Icon(
                  imageVector = Icons.Filled.ErrorOutline,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onErrorContainer,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                  text = errorMsg,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onErrorContainer,
                )
              }
              TextButton(
                onClick = viewModel::retryLastPrompt,
                modifier = Modifier.testTag("ai_retry_button"),
              ) {
                Icon(
                  imageVector = Icons.Filled.Refresh,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text("Retry", fontWeight = FontWeight.Bold)
              }
            }
          }
        }

        // Bottom Prompt Input Bar
        Surface(
          color = MaterialTheme.colorScheme.surface,
          shadowElevation = 4.dp,
          modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
          ) {
            OutlinedTextField(
              value = inputText,
              onValueChange = { inputText = it.take(800) },
              placeholder = { Text("Ask AI about prices, EMI, locations...") },
              singleLine = false,
              maxLines = 3,
              keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
              keyboardActions = KeyboardActions(onSend = { submitPrompt() }),
              shape = RoundedCornerShape(24.dp),
              modifier = Modifier.weight(1f).testTag("ai_prompt_input"),
            )
            IconButton(
              onClick = submitPrompt,
              enabled = inputText.isNotBlank() && !uiState.isGenerating,
              colors =
                IconButtonDefaults.filledIconButtonColors(
                  containerColor = BrokerIndigo,
                  contentColor = Color.White,
                ),
              modifier =
                Modifier.size(48.dp)
                  .clip(CircleShape)
                  .testTag("ai_send_button"),
            ) {
              if (uiState.isGenerating) {
                CircularProgressIndicator(
                  color = Color.White,
                  strokeWidth = 2.dp,
                  modifier = Modifier.size(20.dp),
                )
              } else {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.Send,
                  contentDescription = "Send AI prompt",
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AiMessageBubble(message: AiAssistantMessage) {
  Row(
    horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Surface(
      color = if (message.isUser) BrokerBlue else MaterialTheme.colorScheme.surface,
      shape =
        RoundedCornerShape(
          topStart = 16.dp,
          topEnd = 16.dp,
          bottomStart = if (message.isUser) 16.dp else 4.dp,
          bottomEnd = if (message.isUser) 4.dp else 16.dp,
        ),
      shadowElevation = if (message.isUser) 1.dp else 2.dp,
      modifier = Modifier.widthIn(max = 320.dp),
    ) {
      Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
      ) {
        if (!message.isUser) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Filled.AutoAwesome,
              contentDescription = null,
              tint = BrokerPurple,
              modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(5.dp))
            Text(
              text = "AI Assistant",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = BrokerPurple,
            )
          }
        }
        if (message.text.isBlank() && message.isStreaming) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
              strokeWidth = 2.dp,
              color = BrokerIndigo,
              modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
              text = "Thinking...",
              fontSize = 13.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        } else {
          Text(
            text = message.text,
            fontSize = 14.sp,
            color = if (message.isUser) Color.White else MaterialTheme.colorScheme.onSurface,
          )
        }
      }
    }
  }
}
