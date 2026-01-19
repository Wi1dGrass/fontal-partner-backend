import { defineStore } from 'pinia'
import { ref } from 'vue'

export interface TeamInfo {
  id: number
  name: string
  description?: string
  avatarUrl?: string
  maxNum: number
  expireTime?: string
  userId: number
  status: number
  password?: string
  createTime?: string
  hasPassword: boolean
  joinUserIds?: number[]
  joinNum?: number
}

export const useTeamStore = defineStore('team', () => {
  const myTeams = ref<TeamInfo[]>([])
  const currentTeam = ref<TeamInfo | null>(null)
  const teamList = ref<TeamInfo[]>([])

  function setMyTeams(teams: TeamInfo[]) {
    myTeams.value = teams
  }

  function setCurrentTeam(team: TeamInfo | null) {
    currentTeam.value = team
  }

  function setTeamList(teams: TeamInfo[]) {
    teamList.value = teams
  }

  function addTeam(team: TeamInfo) {
    myTeams.value.push(team)
  }

  function updateTeam(teamId: number, updatedTeam: Partial<TeamInfo>) {
    const index = myTeams.value.findIndex(t => t.id === teamId)
    if (index !== -1) {
      myTeams.value[index] = { ...myTeams.value[index], ...updatedTeam }
    }
    if (currentTeam.value?.id === teamId) {
      currentTeam.value = { ...currentTeam.value, ...updatedTeam }
    }
  }

  function removeTeam(teamId: number) {
    const index = myTeams.value.findIndex(t => t.id === teamId)
    if (index !== -1) {
      myTeams.value.splice(index, 1)
    }
    if (currentTeam.value?.id === teamId) {
      currentTeam.value = null
    }
  }

  return {
    myTeams,
    currentTeam,
    teamList,
    setMyTeams,
    setCurrentTeam,
    setTeamList,
    addTeam,
    updateTeam,
    removeTeam
  }
})
