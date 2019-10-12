package org.cascadebot.cascadebot.data.graphql.services;

import io.leangen.graphql.annotations.GraphQLContext;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.GraphQLRootContext;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.internal.utils.Checks;
import org.cascadebot.cascadebot.CascadeBot;
import org.cascadebot.cascadebot.data.graphql.objects.QLContext;
import org.cascadebot.cascadebot.data.managers.PlaylistManager;
import org.cascadebot.cascadebot.data.objects.Playlist;
import org.cascadebot.cascadebot.data.objects.PlaylistScope;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PlaylistService {

    @Getter
    private static PlaylistService instance = new PlaylistService();

    @GraphQLQuery(description = "Retrieves a playlist from its unique id. This checks that the user has appropriate permissions for the scope (GUILD or USER)")
    public Playlist playlist(@GraphQLRootContext QLContext context, long guildId, @Nonnull String id) {
        Playlist playlist = PlaylistManager.getPlaylistById(id);
        if (playlist == null) return null;
        // Check that the user is authenticated to get the playlist
        if (playlist.getScope() == PlaylistScope.GUILD) {
            return context.runIfAuthenticatedGuild(playlist.getOwnerId(), (guild, member) -> playlist.getOwnerId() == guild.getIdLong() ? playlist : null);
        } else {
            return context.runIfAuthenticatedUser(user -> playlist.getOwnerId() == user.getIdLong() ? playlist : null);
        }
    }

    @GraphQLQuery(description = "Gets all playlists for the specified scope and owner id. If the current user doesn't have permission to access the playlists or the ownerId is invalid, a null value is returned.")
    public List<Playlist> allPlaylists(@GraphQLRootContext QLContext context, long ownerId, @Nonnull PlaylistScope scope) {
        Supplier<List<Playlist>> playlistSupplier = () -> PlaylistManager.getPlaylists(ownerId, scope).into(new ArrayList<>());
        if (scope == PlaylistScope.GUILD) {
            return context.runIfAuthenticatedGuild(ownerId, (guild, member) -> {
                return playlistSupplier.get();
            });
        } else {
            return context.runIfAuthenticatedUser(user -> ownerId == user.getIdLong() ? playlistSupplier.get() : null);
        }
    }

    @GraphQLQuery
    @Nonnull
    public String id(@GraphQLContext Playlist playlist) {
        return playlist.getPlaylistId().toHexString();
    }

    @GraphQLMutation
    public Playlist createPlaylist(@GraphQLRootContext QLContext context, @Nonnull String name, @Nonnull List<String> tracks, long ownerId, @Nonnull PlaylistScope scope) {
        Supplier<Playlist> playlistSupplier = () -> {
            Checks.notBlank(name, "name");
            Checks.notEmpty(tracks, "tracks");

            if (PlaylistManager.getPlaylistByName(ownerId, scope, name) != null) {
                throw new IllegalStateException("This playlist already exists!");
            }

            Playlist playlist = new Playlist(name, tracks, ownerId, scope);
            PlaylistManager.savePlaylist(playlist);
            return playlist;
        };

        if (scope == PlaylistScope.GUILD) {
            return context.runIfAuthenticatedUser((user) -> {
                Guild guild = CascadeBot.INS.getShardManager().getGuildById(ownerId);
                // If the guild exists and the authenticated user is a member of the guild then try and check permissions
                if (guild != null && guild.getMember(user) != null) {
                    boolean hasPermission = context.getGuildData(ownerId)
                                .getGuildPermissions()
                                .hasPermission(
                                        guild.getMember(user),
                                        CascadeBot.INS.getPermissionsManager().getPermission("queue.save"),
                                        context.getGuildData(ownerId).getCoreSettings()
                                );
                    if (hasPermission) {
                        return playlistSupplier.get();
                    } else {
                        throw new RuntimeException("You do not have the permission 'queue.save' to do this!");
                    }
                }
                return null;
            });
        } else {
            return context.runIfAuthenticatedUser((user) -> {
                return playlistSupplier.get();
            });
        }
    }

    @GraphQLMutation
    public String deletePlaylist(@GraphQLRootContext QLContext context, @Nonnull String id) {
        Playlist playlist = PlaylistManager.getPlaylistById(id);
        if (playlist == null) {
            throw new IllegalStateException("There is no playlist by that ID to delete!");
        }
        if (playlist.getScope() == PlaylistScope.GUILD) {

        } else {

        }

        PlaylistManager.deletePlaylistById(id);
        return id;
    }

    @GraphQLMutation(description = "Edits any fields on a playlist. Null fields are skipped.")
    public Playlist editPlaylist(@GraphQLRootContext QLContext context, @Nonnull String id, String name, List<String> tracks, Long ownerId, PlaylistScope playlistScope) {
        Playlist playlist = PlaylistManager.getPlaylistById(id);
        if (playlist == null) {
            throw new IllegalStateException("There is no playlist by that ID to edit! please create one instead!");
        }

        if (name != null) playlist.setName(name);
        if (tracks != null) playlist.setTracks(tracks);
        if (ownerId != null) playlist.setOwnerId(ownerId);
        if (playlistScope != null) playlist.setScope(playlistScope);

        PlaylistManager.replacePlaylist(playlist);

        return playlist;
    }



}
