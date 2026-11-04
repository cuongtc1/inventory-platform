import { PlusOutlined, StopOutlined, UndoOutlined } from '@ant-design/icons'
import { App, Button, Flex, Popconfirm, Select, Tooltip, Typography, type TableColumnsType } from 'antd'
import { Link, useNavigate } from 'react-router'
import { activateUser, deactivateUser, getUsers } from '../../api/users'
import { EditButton } from '../../components/EditButton'
import { PagedTable } from '../../components/PagedTable'
import { PageHeader } from '../../components/PageHeader'
import { SearchFilter } from '../../components/SearchFilter'
import { StatusBadge } from '../../components/StatusBadge'
import { ROUTES } from '../../constants/routes'
import { USER_STATUS, USER_STATUS_OPTIONS, USER_TYPE, USER_TYPE_OPTIONS } from '../../constants/userType'
import { useIsMobile } from '../../hooks/useIsMobile'
import { type PageLoader, usePagedList } from '../../hooks/usePagedList'
import { useReturnState } from '../../hooks/useReturnTo'
import { useSession } from '../../hooks/useSession'
import { texts } from '../../messages/texts'
import type { UserResponse, UserStatus, UserType } from '../../types/auth'
import { errorMessage } from '../../utils/errors'

const DEFAULT_SORT = 'fullName,asc'

/** URL filters → API filters. Outside the component, so usePagedList gets the same function on every render. */
const loadUsers: PageLoader<UserResponse> = (query, filters, signal) =>
  getUsers(
    {
      ...query,
      search: filters.get('search') ?? undefined,
      userType: toUserType(filters.get('userType')),
      status: toStatus(filters.get('status')),
    },
    signal,
  )

/**
 * Users & Roles (ADMIN), like the mockup: Name, Email, Role, Status, actions. Users are never deleted, so the row
 * offers Deactivate / Activate instead of Delete; your own row has neither, because the backend refuses it.
 */
export function UserListPage() {
  const { message } = App.useApp()
  const navigate = useNavigate()
  const isMobile = useIsMobile()
  const session = useSession()
  const returnState = useReturnState()
  const list = usePagedList(loadUsers, DEFAULT_SORT)

  const onDeactivate = async (user: UserResponse) => {
    try {
      await deactivateUser(user.id)
      message.success(texts.user.deactivated(user.email))
      list.reload()
    } catch (error) {
      message.error(errorMessage(error))
    }
  }

  const onActivate = async (user: UserResponse) => {
    try {
      await activateUser(user.id)
      message.success(texts.user.activated(user.email))
      list.reload()
    } catch (error) {
      message.error(errorMessage(error))
    }
  }

  const fields = texts.user.fields
  const columns: TableColumnsType<UserResponse> = [
    {
      title: fields.fullName,
      dataIndex: 'fullName',
      sorter: true,
      sortOrder: list.sortOrder('fullName'),
      render: (fullName: string, user) => (
        <Link to={ROUTES.userEdit(user.id)} state={returnState}>
          {fullName}
        </Link>
      ),
    },
    { title: fields.email, dataIndex: 'email', sorter: true, sortOrder: list.sortOrder('email') },
    {
      title: fields.userType,
      dataIndex: 'userType',
      sorter: true,
      sortOrder: list.sortOrder('userType'),
      render: (userType: UserType) => <StatusBadge value={userType} display={USER_TYPE} />,
    },
    {
      title: fields.status,
      dataIndex: 'status',
      sorter: true,
      sortOrder: list.sortOrder('status'),
      render: (status: UserStatus) => <StatusBadge value={status} display={USER_STATUS} />,
    },
    {
      title: texts.common.fields.actions,
      key: 'actions',
      render: (_, user) => (
        <Flex gap={6}>
          <EditButton to={ROUTES.userEdit(user.id)} />
          {user.id !== session?.user.id && user.status === 'ACTIVE' && (
            <Popconfirm
              title={texts.user.confirmDeactivate(user.fullName)}
              description={texts.user.deactivateHint}
              okText={texts.user.deactivate}
              cancelText={texts.common.actions.cancel}
              okButtonProps={{ danger: true }}
              onConfirm={() => onDeactivate(user)}
            >
              <Tooltip title={isMobile ? texts.user.deactivate : undefined}>
                <Button size="small" danger icon={<StopOutlined />} aria-label={texts.user.deactivate}>
                  {!isMobile && texts.user.deactivate}
                </Button>
              </Tooltip>
            </Popconfirm>
          )}
          {user.status === 'INACTIVE' && (
            <Tooltip title={isMobile ? texts.user.activate : undefined}>
              <Button size="small" icon={<UndoOutlined />} aria-label={texts.user.activate} onClick={() => onActivate(user)}>
                {!isMobile && texts.user.activate}
              </Button>
            </Tooltip>
          )}
        </Flex>
      ),
    },
  ]

  const filterWidth = (desktop: number) => ({ width: isMobile ? '100%' : desktop })

  return (
    <>
      <PageHeader
        title={texts.user.title}
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate(ROUTES.userNew, { state: returnState })}>
            {texts.user.newButton}
          </Button>
        }
      />
      <PagedTable
        list={list}
        columns={columns}
        filters={
          <>
            <SearchFilter list={list} placeholder={texts.user.searchPlaceholder} />
            <Select
              value={toUserType(list.filter('userType'))}
              options={USER_TYPE_OPTIONS}
              placeholder={texts.user.allRoles}
              allowClear
              onChange={(userType?: UserType) => list.setFilters({ userType })}
              style={filterWidth(160)}
            />
            <Select
              value={toStatus(list.filter('status'))}
              options={USER_STATUS_OPTIONS}
              placeholder={texts.common.filters.allStatuses}
              allowClear
              onChange={(status?: UserStatus) => list.setFilters({ status })}
              style={filterWidth(160)}
            />
          </>
        }
      />
      <Typography.Paragraph type="secondary" style={{ marginTop: 14 }}>
        {texts.user.roleNote}
      </Typography.Paragraph>
    </>
  )
}

/** Only values the frontend knows; anything else in the URL is ignored. */
function toUserType(value: string | null | undefined): UserType | undefined {
  return value !== null && value !== undefined && value in USER_TYPE ? (value as UserType) : undefined
}

function toStatus(value: string | null | undefined): UserStatus | undefined {
  return value !== null && value !== undefined && value in USER_STATUS ? (value as UserStatus) : undefined
}
