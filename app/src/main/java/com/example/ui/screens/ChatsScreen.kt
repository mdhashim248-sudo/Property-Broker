package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.UiState
import com.example.data.model.ChatMessage
import com.example.data.model.PropertyChat
import com.example.ui.theme.BrokerBlue
import com.example.ui.theme.BrokerIndigo
import com.example.ui.theme.ChatBgLight
import com.example.ui.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsListScreen(
  chatsState: UiState<List<PropertyChat>>,
  currentUserId: String,
  onBack: () -> Unit,
  onOpenChat: (String) -> Unit,
) {
  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = { Text("Chats", fontWeight = FontWeight.Bold) },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = BrokerIndigo,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
          ),
      )
    },
  ) { innerPadding ->
    Box(
      contentAlignment = Alignment.TopCenter,
      modifier = Modifier.fillMaxSize().padding(innerPadding).background(MaterialTheme.colorScheme.background),
    ) {
      Column(modifier = Modifier.widthIn(max = 640.dp).fillMaxSize()) {
        when (chatsState) {
          is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              CircularProgressIndicator()
            }
          }
          is UiState.Error -> {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
              Text(chatsState.message, color = MaterialTheme.colorScheme.error)
            }
          }
          is UiState.Success -> {
            val chats = chatsState.data
            if (chats.isEmpty()) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize().padding(24.dp),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.Chat,
                  contentDescription = null,
                  tint = BrokerIndigo,
                  modifier = Modifier.size(48.dp),
                )
                Spacer(Modifier.height(12.dp))
                Text("No chats yet", style = MaterialTheme.typography.titleMedium, color = BrokerIndigo)
                Spacer(Modifier.height(6.dp))
                Text(
                  text = "Open any property and tap Chat with Owner to start messaging.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            } else {
              LazyColumn(
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
              ) {
                items(chats, key = { it.id }, contentType = { "chat_row" }) { chat ->
                  val counterpart =
                    if (chat.buyerId == currentUserId) chat.ownerName else chat.buyerName
                  Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier =
                      Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onOpenChat(chat.id) },
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(12.dp),
                      modifier = Modifier.padding(14.dp),
                    ) {
                      Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                        modifier = Modifier.size(44.dp),
                      ) {
                        Box(contentAlignment = Alignment.Center) {
                          Text(
                            text = counterpart.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = BrokerIndigo,
                          )
                        }
                      }
                      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                          text = chat.propertyTitle,
                          style = MaterialTheme.typography.titleMedium,
                          fontWeight = FontWeight.Bold,
                          color = BrokerIndigo,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                          text = "$counterpart: ${chat.lastMessage}",
                          style = MaterialTheme.typography.bodySmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun ChatThreadScreen(
  viewModel: ChatViewModel,
  currentUserName: String,
  onMessageSent: (String) -> Unit = {},
  onBack: () -> Unit,
) {
  val chat by viewModel.chatState.collectAsStateWithLifecycle()
  val messagesState by viewModel.messagesState.collectAsStateWithLifecycle()
  val isSending by viewModel.isSending.collectAsStateWithLifecycle()
  val errorBanner by viewModel.errorBanner.collectAsStateWithLifecycle()

  ChatThreadContent(
    chat = chat,
    messagesState = messagesState,
    currentUserId = viewModel.currentUserId,
    isSending = isSending,
    errorBanner = errorBanner,
    onSendMessage = { text, onSent ->
      onMessageSent(text)
      viewModel.sendMessage(currentUserName, text, onSent)
    },
    onBack = onBack,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatThreadContent(
  chat: PropertyChat?,
  messagesState: UiState<List<ChatMessage>>,
  currentUserId: String,
  isSending: Boolean,
  errorBanner: String?,
  onSendMessage: (text: String, onSent: () -> Unit) -> Unit,
  onBack: () -> Unit,
) {
  var draft by remember { mutableStateOf("") }
  val listState = rememberLazyListState()

  val messages = (messagesState as? UiState.Success)?.data.orEmpty()
  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.size - 1)
    }
  }

  Scaffold(
    containerColor = ChatBgLight,
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = chat?.propertyTitle ?: "Chat with Owner",
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = BrokerIndigo,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
          ),
      )
    },
  ) { innerPadding ->
    Box(
      contentAlignment = Alignment.TopCenter,
      modifier = Modifier.fillMaxSize().padding(innerPadding).background(ChatBgLight).imePadding(),
    ) {
      Column(modifier = Modifier.widthIn(max = 640.dp).fillMaxSize()) {
        if (errorBanner != null) {
          Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = errorBanner,
              color = MaterialTheme.colorScheme.onErrorContainer,
              style = MaterialTheme.typography.bodySmall,
              modifier = Modifier.padding(10.dp),
            )
          }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
          when (messagesState) {
            is UiState.Loading -> {
              CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is UiState.Error -> {
              Text(
                text = messagesState.message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.Center).padding(20.dp),
              )
            }
            is UiState.Success -> {
              if (messages.isEmpty()) {
                Text(
                  text = "Type a message to start chatting...",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.align(Alignment.Center).padding(24.dp),
                )
              } else {
                LazyColumn(
                  state = listState,
                  contentPadding = PaddingValues(12.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                  modifier = Modifier.fillMaxSize(),
                ) {
                  items(messages, key = { it.id }, contentType = { "chat_bubble" }) { msg ->
                    ChatBubble(
                      message = msg,
                      isMine = msg.senderId == currentUserId,
                    )
                  }
                }
              }
            }
          }
        }

        // White elevated bottom bar with Send button matching activity_chat.xml
        Surface(
          color = Color.White,
          shadowElevation = 4.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(10.dp),
          ) {
            OutlinedTextField(
              value = draft,
              onValueChange = { draft = it.take(1000) },
              placeholder = { Text("Type a message...") },
              maxLines = 3,
              shape = MaterialTheme.shapes.small,
              modifier = Modifier.weight(1f).testTag("chat_message_input"),
            )
            Button(
              onClick = {
                onSendMessage(draft) { draft = "" }
              },
              enabled = draft.isNotBlank() && !isSending,
              shape = MaterialTheme.shapes.small,
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = BrokerBlue,
                  contentColor = Color.White,
                ),
              modifier = Modifier.height(52.dp).testTag("chat_send_button"),
            ) {
              Text("Send", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

private val chatTimeFormatter =
  object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue(): SimpleDateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
  }

@Composable
private fun ChatBubble(message: ChatMessage, isMine: Boolean) {
  val formattedTime =
    remember(message.id, message.createdAt) {
      message.createdAt?.toDate()?.let { chatTimeFormatter.get()?.format(it) } ?: "Sending"
    }

  Column(
    horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Text(
      text = "${message.senderName} • $formattedTime",
      style = MaterialTheme.typography.labelSmall,
      color = Color(0xFF616161),
      modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
    )
    Surface(
      color = if (isMine) BrokerBlue else Color.White,
      shape =
        RoundedCornerShape(
          topStart = 14.dp,
          topEnd = 14.dp,
          bottomStart = if (isMine) 14.dp else 4.dp,
          bottomEnd = if (isMine) 4.dp else 14.dp,
        ),
      shadowElevation = 1.dp,
    ) {
      Text(
        text = message.text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (isMine) Color.White else Color(0xFF212121),
        modifier = Modifier.widthIn(max = 280.dp).padding(horizontal = 12.dp, vertical = 9.dp),
      )
    }
  }
}
