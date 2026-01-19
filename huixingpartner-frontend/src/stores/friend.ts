import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { UserInfo } from './user'

export interface FriendRequest {
  id: number
  fromId: number
  toId: number
  fromUser?: UserInfo
  toUser?: UserInfo
  status: number
  createTime?: string
}

export const useFriendStore = defineStore('friend', () => {
  const friendList = ref<UserInfo[]>([])
  const receivedRequests = ref<FriendRequest[]>([])
  const sentRequests = ref<FriendRequest[]>([])
  const unreadRequestCount = ref(0)

  function setFriendList(list: UserInfo[]) {
    friendList.value = list
  }

  function setReceivedRequests(requests: FriendRequest[]) {
    receivedRequests.value = requests
    updateUnreadCount()
  }

  function setSentRequests(requests: FriendRequest[]) {
    sentRequests.value = requests
  }

  function addFriend(friend: UserInfo) {
    friendList.value.push(friend)
  }

  function removeFriend(userId: number) {
    const index = friendList.value.findIndex(f => f.id === userId)
    if (index !== -1) {
      friendList.value.splice(index, 1)
    }
  }

  function addReceivedRequest(request: FriendRequest) {
    receivedRequests.value.unshift(request)
    updateUnreadCount()
  }

  function addSentRequest(request: FriendRequest) {
    sentRequests.value.unshift(request)
  }

  function updateRequestStatus(requestId: number, status: number) {
    const index = receivedRequests.value.findIndex(r => r.id === requestId)
    if (index !== -1) {
      receivedRequests.value[index].status = status
      updateUnreadCount()
    }
  }

  function removeRequest(requestId: number) {
    const receivedIndex = receivedRequests.value.findIndex(r => r.id === requestId)
    if (receivedIndex !== -1) {
      receivedRequests.value.splice(receivedIndex, 1)
      updateUnreadCount()
    }
    const sentIndex = sentRequests.value.findIndex(r => r.id === requestId)
    if (sentIndex !== -1) {
      sentRequests.value.splice(sentIndex, 1)
    }
  }

  function updateUnreadCount() {
    unreadRequestCount.value = receivedRequests.value.filter(
      r => r.status === 0
    ).length
  }

  return {
    friendList,
    receivedRequests,
    sentRequests,
    unreadRequestCount,
    setFriendList,
    setReceivedRequests,
    setSentRequests,
    addFriend,
    removeFriend,
    addReceivedRequest,
    addSentRequest,
    updateRequestStatus,
    removeRequest
  }
})
